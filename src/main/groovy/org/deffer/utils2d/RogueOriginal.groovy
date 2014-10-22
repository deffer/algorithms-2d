package org.deffer.utils2d;

def randomInt = { int min, int max ->
	return (int) (Math.random()*(max-min))+min;
}


def w = 300, h = 300;
def cw = 5, ch = 5;

def cwp = Math.floor(w/cw); // number of units/pixels in each cell width
def chp = Math.floor(h/ch); // number of units/pixels in each cell height

def cgx = 0, cgy = 0;

def connectedCells = [];
def map = [];
def roomSizes = [];
def rooms = [];
def room;
def connection;
def xOffset;
def yOffset;


def wall;
def connectingRoom;
def otherRoom;
def otherWall;

def wallx;
def wally;

// room.x
// room.y
// room.width
// room.heigh
// room.connections = [ [x,y], [x,y] ]

// 3 and 7 duplicates?
def dir = [[0,1], [1,1], [1,0], [1,-1], [0,-1], [-1, -1], [-1,0], [-1, -1]];

// create map
(0..w-1).each{ i->
	map[i] = []
	(0..h-1).each{j -> map[i][j] = 1}
}

// create "grid"
def grid = [];
(0..cw-1).each {i->
	rooms[i] = []
	(0..ch-1).each {j->
		rooms[i][j] = ["x":0, "y":0, "width":0, "height":0, "connections":[], "cellx":i, "celly":j];
	}
}


//pick random starting grid
//var currentGrid = ROT.RNG.getRandomInt(1, (cw * ch)) - 1;
//console.log("First grid: " + currentGrid);
cgx = randomInt(0, cw-1)
cgy = randomInt(0, ch-1)

//grid[cgx][cgy] = false; // mark as connected;

//connectedCells.push([cgx,cgy]);

def idx, ncgx, ncgy

// find  unconnected neighbour cells
while (true){

	def dirToCheck = [0,2,4,6];
	Collections.shuffle(dirToCheck);

	while (true){
		def found = false;
		idx = dirToCheck.pop();

		ncgx = cgx + dir[idx][0];
		ncgy = cgy + dir[idx][1];

		if(ncgx < 0 || ncgx >= cw) continue;
		if(ncgy < 0 || ncgy >= ch) continue;

		room = rooms[cgx][cgy];
		if(room["connections"].size() > 0){
			if(room["connections"][0][0] == ncgx &&
					room["connections"][0][1] == ncgy){
				break;
			}
		}

		otherRoom = rooms[ncgx][ncgy];

		if(otherRoom["connections"].size() == 0){
			// console.log("Connecting cell:" + cgx + "," + cgy + " to " + ncgx + "," + ncgy);
			// set this as the current cell.
			//grid[ncgx][ncgy] = [cgx, cgy];

			otherRoom["connections"] << [cgx,cgy];

			connectedCells << [ncgx, ncgy];
			cgx = ncgx;
			cgy = ncgy;
			found = true;
		}
		if (dirToCheck.size() <= 0 || found) break;
	}

	if (dirToCheck.size() <= 0) break;
}


// console.log("Now checking for unconnected cells");

//While there are unconnected rooms, try to connect them to a random connected neighbor (if a room has no connected neighbors yet, just keep cycling, you'll fill out to it eventually).

def randomConnectedCell;
Collections.shuffle(connectedCells)

(0..cw-1).each {i->
	(0..ch-1).each{j->

		room = rooms[i][j];

		if(room["connections"].size() == 0){
			def directions = [0,2,4,6];
			Collections.shuffle(directions)

			def validRoom = false;

			while(directions.size()>0){

				def dirIdx = directions.pop();
				def newI = i + dir[dirIdx][0];
				def newJ = j + dir[dirIdx][1];

				if(newI < 0 || newI >= cw || newJ < 0 || newJ >= ch){
					continue;
				}

				otherRoom = rooms[newI][newJ];

				validRoom = true;

				if(otherRoom["connections"].size() == 0)
					break;


				otherRoom["connections"].each{ def entry ->
					if(validRoom && entry[0] == i && entry[1] == j){
						validRoom = false;
					}
				}

				if(validRoom) break;
			}

			if(validRoom)
				room["connections"] << [otherRoom["cellx"], otherRoom["celly"]] ;
			else
				println("-- Unable to connect room.");

		}
	}
}

//Make 0 or more random connections to taste; I find rnd(grid_width) random connections looks good.
// later


// Create Rooms

int roomw;
int roomh;
def roomWidth = [20,50];//this._options["roomWidth"];
def roomHeight = [20, 50];//this._options["roomHeight"];


(0..cw-1).each {i->
	(0..ch-1).each{j->
		int sx = cwp * i;
		int sy = chp * j;

		if(sx == 0) sx = 1;
		if(sy == 0) sy = 1;

		roomw = randomInt(roomWidth[0], roomWidth[1]);
		roomh = randomInt(roomHeight[0], roomHeight[1]);

		if(j > 0){
			otherRoom = rooms[i][j-1];
			while(sy - (otherRoom["y"] + otherRoom["height"] ) < 3){
				sy++;
			}
		}

		if(i > 0){
			otherRoom = rooms[i-1][j];
			while(sx - (otherRoom["x"] + otherRoom["width"]) < 3){
				sx++;
			}
		}

		def sxOffset = Math.round(randomInt(0, ((int)cwp-roomw))/2);
		def syOffset = Math.round(randomInt(0, ((int)chp-roomh))/2);

		while(sx + sxOffset + roomw >= w) {
			if(sxOffset) sxOffset--; else roomw--;
		}

		while(sy + syOffset + roomh >= h) {
			if(syOffset) syOffset--; else roomh--;
		}

		sx = sx + sxOffset;
		sy = sy + syOffset;

		rooms[i][j]["x"] = sx;
		rooms[i][j]["y"] = sy;
		rooms[i][j]["width"] = roomw;
		rooms[i][j]["height"] = roomh;

		for(int ii = sx; ii < sx + roomw; ii++){
			for(int jj = sy; jj < sy + roomh; jj++){
				map[ii][jj] = 0;
			}
		}
	}
}

// Draw Corridors between connected rooms
(0..cw-1).each {i->
	(0..ch-1).each{j->

		room = rooms[i][j];

		//console.log(room);
		room["connections"].each {entry->

			connection = entry; // this is a 2 item array.

			// console.log(connection);

			otherRoom = rooms[connection[0]][connection[1]];

			// figure out what wall our corridor will start one.
			// figure out what wall our corridor will end on.
			if(otherRoom["cellx"] > room["cellx"] ){
				wall = 2;
				otherWall = 4;
			} else if(otherRoom["cellx"] < room["cellx"] ){
				wall = 4;
				otherWall = 2;
			}else if(otherRoom["celly"] > room["celly"]){
				wall = 3;
				otherWall = 1;
			}else if(otherRoom["celly"] < room["celly"]){
				wall = 1;
				otherWall = 3;
			}

			def getWallPosition = {aRoom, aDirection->
				def rx, ry, door

				if(aDirection == 1 || aDirection == 3){
					rx = randomInt(aRoom["x"] + 1, aRoom["x"] + aRoom["width"] - 2);
					if(aDirection == 1){
						ry = aRoom["y"] - 2;
						door = ry + 1;
					}else{
						ry = aRoom["y"] + aRoom["height"] + 1;
						door = ry -1;
					}

					map[rx][door] = 0;
				}else if(aDirection == 2 || aDirection == 4){
					ry = randomInt(aRoom["y"] + 1, aRoom["y"] + aRoom["height"] - 2);
					if(aDirection == 2){
						rx = aRoom["x"] + aRoom["width"] + 1;
						door = rx - 1;
					}else{
						rx = aRoom["x"] - 2;
						door = rx + 1;
					}

					map[door][ry] = 0;
				}
				if (rx >= 300 || ry >= 300){
					println "For ($rx,$ry) Direction $aDirection, x,y=${aRoom['x']},${aRoom['y']}  w,h=${aRoom['width']},${aRoom['height']}"
				}
				return [rx, ry];
			}

			def drawCorridore = {startPosition, endPosition->
				//console.log("drawCorridore " );
				//println("From " + startPosition[0] + "," + startPosition[1]+"  To "+endPosition[0] + "," + endPosition[1]);

				xOffset = endPosition[0] - startPosition[0];
				yOffset = endPosition[1] - startPosition[1];

				def xCount = 0, yCount = 0;
				def counter = 0;
				def tx, ty
				def path = [];

				if(xOffset != 0)
					xCount = xOffset > 0 ? 1 : -1;

				if(yOffset != 0)
					yCount = yOffset > 0 ? 1 : -1;

				if (startPosition[0] < 300)
					map[startPosition[0]][startPosition[1]] = 0;
				if (endPosition[0] < 300)
					map[endPosition[0]][endPosition[1]] = 0;

				while(xOffset != 0 || yOffset != 0){
					if(xOffset != 0 && counter == 0){
						counter = 1;
						xOffset -= xCount;
					}else if(yOffset != 0 && counter == 1){
						counter = 0;
						yOffset -= yCount;
					}

					if(xOffset == 0) counter = 1;
					if(yOffset == 0) counter = 0;

					tx = endPosition[0] - xOffset;
					ty = endPosition[1] - yOffset;

					map[tx][ty] = 0;
					//map[tx-xCount][ty] = 0;
				}
			}

			//console.log("dir: " + wall + " wall 1: " + getWallPosition(room, wall) + " dir " + otherWall + " wall 2: " + getWallPosition( otherRoom, otherWall));
			drawCorridore(getWallPosition(room, wall), getWallPosition(otherRoom, otherWall));
		}

	}
}


new File("d:\\data\\dung.txt").withWriter {writer ->
	(0..w-1).each {i->
		StringBuilder sb = new StringBuilder(500)
		(0..h-1).each {j->
			sb.append(map[i][j]==0? ".":"*")
		}
		writer << sb.toString()+"\n"
	}
}

