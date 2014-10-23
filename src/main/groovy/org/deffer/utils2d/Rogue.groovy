package org.deffer.utils2d

public class Rogue{
	public static final int TILE_TERRAIN = 0 // rooms, corridors
	public static final int TILE_EMPTY = 1   // unreachable
	public static final int TILE_WALL = 2    // wall
	public static final int TILE_PATH = 3    // for debug
	public static final int TILE_CELL = 4    // for debug



	// ----------------------------------------------------
	// Values that are configuring map being generated
	// ----------------------------------------------------
	public def w = 80, h = 80;
	public def cw = 4, ch = 4;
	public def roomWidth = [10, 25];  // min max.  MUST be less than (cwp*2 - cwp/3)
	public def roomHeight = [10, 25];
	boolean debugSteps = false;
	// other options
	/*def w = 300, h = 300;
	def cw = 5, ch = 5;
	def roomWidth = [30, 80];  // min max.  MUST be less than (cwp*2 - cwp/3)
	def roomHeight = [30, 80]; // min max
	*/



	// we going to use this often
	def cwp = Math.floor(w/cw); // cell width in pixels
	def chp = Math.floor(h/ch); // cell height in pixels
	int deviationx = Math.round((Math.floor(cwp/6)))  // should probably also depend on map size
	int deviationy = Math.round((Math.floor(chp/6)))  // should probably also depend on map size

	// internal structures
	def map = []         // map of tiles/pixels of given width/height (w,h)
	def debugMap = []    // same as map, just more detailed
	def rooms = []       // a map of rooms (cw,ch)
	def roomsList = []   // a list of rooms from the rooms map, for easier iteration
	def connections = [] // list of edges between cells. although it has 'from' and 'to', its treated as undirected
	def start, end;      // usually this will be the longest path through the map

	public static int randomInt (int min, int max){
		return (int) (Math.random()*(max-min))+min;
	}

	// in the range array[idxFrom]..arrays[idxTo] find all non-zero values and return their indices
	// the lowest value's index will be the first. order of the rest is not important
	public static List getIndicesOfNonZeroValues (int idxFrom, int idxTo, List array){
		def result = []
		for (int i = idxFrom; i<= idxTo; i++){
			if (array[i] > 0){
				if (result.size() > 0 && array[result.get(0)]>array[i])
					result.add(0, i)
				else
					result << i
			}
		}
		return result
	}

	public static Map roomCopy (from, to){
		to.x = from.x;         to.y = from.y;
		to.width = from.width; to.height = from.height;
		to.cellx = from.cellx; to.celly = from.celly;
		return to
	}


	// given desired room dimensions, tries to squish it between bordering rooms.
	// grid contains all rooms, but not all of them have dimensions defined yet
	public fitRoom = {room, grid ->

		int maxCellx = grid.size() - 1
		int maxCelly = grid[0].size()-1

		// callback function. returns cell, or null if its out of grid boundaries or doesnt have dimensions yet
		def getCellSafe = {p ->
			def otherX = room.cellx + p.x, otherY = room.celly + p.y
			if (otherX >= 0 && otherX <= maxCellx && otherY >= 0 && otherY <= maxCelly){
				def cell = grid[otherX][otherY]
				if (cell.width>0 && cell.height>0)
					return cell
				else
					return null
			}else
				return null
		}

		// get all neighbours that have dimensions defined
		def cells = Grid.dir.collect(getCellSafe).findAll {return it}
		if (cells.isEmpty()) {
			println ("Adjusting room at ${room.cellx},${room.celly} [${room.x},${room.y} - ${room.width}x${room.height}] SKIP - no neigbours yet")
			return
		}else if (debugSteps){
			println ("Adjusting room at ${room.cellx},${room.celly} [${room.x},${room.y} ${room.width}x${room.height}]")
			String str = "    "
			cells.each{
				str += " ${it.cellx},${it.celly} - [${it.x},${it.y} ${it.width}x${it.height}]."
			}
			println str
		}

		// detect overlap depth for all 4 sides, keep adjusting room dimensions 1 side at a time until there are no overlaps anywhere
		int counter = 0
		List overlaps = Grid.getSidesOverlaps(room, cells)
		while (overlaps[0] > 0){

			// try adjusting every "offending" side and pick the best result
			def bestOverlaps = overlaps
			def bestResult = room
			def idxs = getIndicesOfNonZeroValues(1,5, overlaps)
			idxs.each {idx ->
				def room1 = roomCopy(room, [:])
				Grid.adjustRectangle(room1, idx, 1)
				def ov1 = Grid.getSidesOverlaps(room1, cells)
				if (ov1[0] < bestOverlaps[0]){
					bestOverlaps = ov1;
					bestResult = room1
				} else if (ov1[0] > overlaps[0]){
					println "Algorithm fail... making overlap worse!"
					room1 = roomCopy(room, [:]) // for debug
					Grid.adjustRectangle(room1, idx, 1)   // for debug
					ov1 = Grid.getSidesOverlaps(room1, cells) // for debug
				}
			}

			double ratio = (bestResult.width / bestResult.height)
			if ( ratio > 3 || ratio < 0.3)
				if (debugSteps) println "    Squishing too much: [${bestResult.x},${bestResult.y} ${bestResult.width}x${bestResult.height}]"

			counter ++

			if (counter > 5000){
				println "    Current value is [${bestResult.x},${bestResult.y} ${bestResult.width}x${bestResult.height}]"
				throw new Exception("Algorithm fail... Stuck in a loop adjusting room boundaries. ")
			}

			roomCopy(bestResult, room)
			overlaps = bestOverlaps
		}

		if (debugSteps)
			println ("    Result ${room.cellx},${room.celly} [${room.x},${room.y} - ${room.width}x${room.height}]")
	}


	public void drawCorridor (from, to, map, boolean debug){
		Grid.bresenham(from, to) {x, y->
			if (debug)
				map[x][y] = TILE_PATH
			else {
				// draw a rectangle with the center in the x,y
				def surroundCells = Grid.getSurroundingCells(x, y, w-1, h-1)
				map[x][y] = TILE_TERRAIN
				surroundCells.each {p->
					map[p.x][p.y] = TILE_TERRAIN
				}
			}
		}
	}

	private void init(){
		if (roomWidth[1] > cwp*1.5)
			throw new Exception("Max room width is too big. Should not be more that 1.5 of cell's width")

		if (roomHeight[1] > chp*1.5)
			throw new Exception("Max room height is too big. Should not be more that 1.5 of cell's height")

		// initialize map
		(0..w-1).each{ i->
			map[i] = []
			(0..h-1).each{j -> map[i][j] = TILE_EMPTY}
		}

		// initialize grid
		(0..cw-1).each {i->
			rooms[i] = []
			(0..ch-1).each {j->
				def room = ["x":0, "y":0, "width":0, "height":0, "connections":[], "cellx":i, "celly":j];
				rooms[i][j] = room
				roomsList << room
			}
		}
	}


	public List generate() {
		init()

		//pick random starting cell/room
		def currentRoom = rooms[randomInt(0, cw - 1)][randomInt(0, ch - 1)];
		start = currentRoom

		// Form an acyclic graph: find unconnected neighbour cell, connect with it, jump to it and repeat
		while (currentRoom != null) {
			// TODO replace with Grid.getSurroundingCells
			def dirToCheck = (0..6).toList(); // TODO error? should be 0..7 ?
			Collections.shuffle(dirToCheck);
			def found = false
			while (!dirToCheck.isEmpty()) {
				def idx = dirToCheck.pop();
				def p = Grid.dir[idx]
				def otherX = currentRoom.cellx + p.x
				def otherY = currentRoom.celly + p.y
				if (otherX < 0 || otherX >= cw) continue;
				if (otherY < 0 || otherY >= ch) continue;

				def otherRoom = rooms[otherX][otherY]
				if (otherRoom.connections.size() == 0) {
					// connect to this room
					currentRoom.connections << [x: otherRoom.cellx, y: otherRoom.celly]
					otherRoom.connections << [x: currentRoom.cellx, y: currentRoom.celly]
					connections << [from: currentRoom, to: otherRoom]
					currentRoom = otherRoom
					found = true
					break;
				}
			}

			if (!found) currentRoom = null
		}

		//While there are unconnected rooms, try to connect them to a random connected neighbor
		// (if a room has no connected neighbors yet, just keep cycling, you'll fill out to it eventually).
		def unconnected = new LinkedList(roomsList.findAll { room -> return room.connections.size() == 0 })
		while (unconnected.size() > 0) {

			currentRoom = unconnected.pop()
			def found = false;
			def dirToCheck = (0..6).toList();  // TODO error? should be 0..7
			Collections.shuffle(dirToCheck);
			while (!dirToCheck.isEmpty()) {
				def idx = dirToCheck.pop();
				def p = Grid.dir[idx]
				def otherX = currentRoom.cellx + p.x, otherY = currentRoom.celly + p.y
				if (otherX < 0 || otherX >= cw) continue;
				if (otherY < 0 || otherY >= ch) continue;
				def otherRoom = rooms[otherX][otherY]
				if (otherRoom.connections.size() > 0) {
					// connect to it
					currentRoom.connections << [x: otherRoom.cellx, y: otherRoom.celly]
					otherRoom.connections << [x: currentRoom.cellx, y: currentRoom.celly]
					connections << [from: currentRoom, to: otherRoom]
					found = true
					end = currentRoom
					break;
				}
			}
			if (!found) unconnected.addFirst(currentRoom)
		}

		//Make 0 or more random connections to taste; I find rnd(grid_width) random connections looks good.
		// TODO

		// Create Rooms

		// for every cell, get a center (or left corner) and randomly deviate from it, lets say PX,PY
		//   pick random room size, and place its center (or left corner) in the PX, PY.
		//   adjust room size to fit between other rooms and borders of the map
		Collections.shuffle(roomsList)
		roomsList.each { room ->
			// desired room boundaries
			def roomw = randomInt(roomWidth[0], roomWidth[1]);
			def roomh = randomInt(roomHeight[0], roomHeight[1]);

			// desired coordinates
			int fromx = (cwp * room.cellx) + randomInt(-deviationx, deviationx);
			int fromy = (chp * room.celly) + randomInt(-deviationy, deviationy);
			room.x = fromx; room.y = fromy; room.width = roomw; room.height = roomh

			// adjust to map boundaries
			if (room.x <= 0) room.x = 1;
			if (room.y <= 0) room.y = 1;
			if (room.x + room.width - 1 >= w - 1) room.width = (w - 3 - room.x)
			if (room.y + room.height - 1 >= h - 1) room.height = (h - 3 - room.y)

			// adjust room boundaries in respect to neighbors and map borders
			fitRoom(room, rooms)

			int tox = room.x + room.width - 1
			int toy = room.y + room.height - 1

			for (int ii = room.x; ii <= tox; ii++) {
				for (int jj = room.y; jj <= toy; jj++) {
					// if (ii == room.x || ii == tox-1 || jj == room.y || jj == toy-1) // to get bordering walls instead of shared walls
					if (ii == room.x || ii == tox || jj == room.y || jj == toy)
						map[ii][jj] = TILE_WALL;
					else
						map[ii][jj] = TILE_TERRAIN;
				}
			}
		}

		// copy rooms into debug map
		(0..w - 1).each { i ->
			debugMap[i] = []
			(0..h - 1).each { j -> debugMap[i][j] = map[i][j] }
		}

		// Draw/dig Corridors between connected rooms (center to center)
		connections.each { edge ->
			def room = edge.from
			def otherRoom = edge.to

			drawCorridor(Grid.getRectCenter(room), Grid.getRectCenter(otherRoom), debugMap, true)
			drawCorridor(Grid.getRectCenter(room), Grid.getRectCenter(otherRoom), map,false)
		}

		// add walls to corridors
		// doing it by replacing any EMPTY pixel contacting with TERRAIN with WALL

		// debug cell's boundaries
		roomsList.each { room ->
			int fromx = (cwp * room.cellx)
			int fromy = (chp * room.celly)
			int tox = fromx + cwp - 1
			int toy = fromy + chp - 1
			if (tox >= w) tox = w - 1
			if (toy >= h) toy = h - 1

			Grid.bresenham([x: fromx, y: fromy], [x: tox, y: fromy]) { x, y -> debugMap[x][y] = TILE_CELL }
			Grid.bresenham([x: fromx, y: fromy], [x: fromx, y: toy]) { x, y -> debugMap[x][y] = TILE_CELL }
			Grid.bresenham([x: tox, y: fromy], [x: tox, y: toy]) { x, y -> debugMap[x][y] = TILE_CELL }
			Grid.bresenham([x: tox, y: toy], [x: fromx, y: toy]) { x, y -> debugMap[x][y] = TILE_CELL }
		}

		return map
	}

	public void writeDown(){
		new File("g:\\dev\\dung_debug.txt").withWriter {writer ->
			def chars = ['.', '*','@', '#', 'o']
			(0..w-1).each {i->
				StringBuilder sb = new StringBuilder(500)
				(0..h-1).each {j->
					sb.append(chars[ debugMap[i][j] ])
				}
				writer << sb.toString()+"\n"
			}
		}


		new File("g:\\dev\\dung.txt").withWriter {writer ->
			def chars = ['.', '*','@']
			(0..w-1).each {i->
				StringBuilder sb = new StringBuilder(500)
				(0..h-1).each {j->
					sb.append(chars[ map[i][j] ])
				}
				writer << sb.toString()+"\n"
			}
		}
	}

	public static void main(String[] args){
		Rogue instance = new Rogue(debugSteps: true)
		instance.generate()
		instance.writeDown()
	}

}
