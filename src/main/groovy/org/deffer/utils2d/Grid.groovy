package org.deffer.utils2d

public class Grid {

	// ----------------------------
	// Constants
	// ----------------------------

	// surrounding cells
	static def dir = [[x: 0,y:1], [x:1,y:1], [x:1,y:0], [x:1,y:-1], [x:0,y:-1], [x:-1, y:-1], [x:-1,y:0], [x:-1, y:1]];

	static def dirRight = [[x:  1,y:-1], [x:1,y:0], [x:1,y:1]];  // cells on the right
	static def dirLeft  = [[x: -1,y:-1], [x:-1,y:0], [x:-1,y:1]];
	static def dirUp    = [[x: -1,y:-1], [x:0,y:-1], [x:1,y:-1]];
	static def dirDown  = [[x: -1,y: 1], [x:0,y:1], [x:1,y:1]];


	//       1
	//    +----+
	//  4 |    | 2
	//    +----+
	public static final int TOP=1, RIGHT=2, BOTTOM=3, LEFT=4

	public static int randomInt (int min, int max){
		return (int) (Math.random()*(max-min))+min;
	}

	// returns true if n is between a and b (inclusive)
	public static boolean between (int n, int a, int b){
		return n>=a && n<=b
	}

	// returns list of x,y objects that are 'around' given x,y.
	public static List getSurroundingCells(int x, int y, int maxx, int maxy){
		def result = []

		def directions = (0..7).toList();

		directions.each {i  ->
			def p = dir[i]
			def otherX = x + p.x, otherY = y + p.y
			if (otherX >= 0 && otherX < maxx && otherY >= 0 && otherY < maxy) {
				result << [x: otherX, y: otherY]
			}
		}
		return result
	}

	public static Map getRectCenter (rect){
		int x = rect.x +  Math.round(Math.floor(rect.width / 2))
		int y = rect.y +  Math.round(Math.floor(rect.height / 2))
		return [x: x, y: y]
	}

	// shrinks rectangle by moving one side by <step> pixels
	// rect fields: x,y,width,height
	// sideIdx: LEFT, TOP, etc...
	public static boolean adjustRectangle (rect, sideIdx, step){

		switch (sideIdx){
			case LEFT: if (rect.width > 0){
				rect.x += step
				rect.width -= step
				return true
			}
				break;
			case RIGHT: if (rect.width > 0){
				rect.width -= step
				return true
			}
				break;
			case TOP: if (rect.height > 0){
				rect.y += step
				rect.height -= step
				return true
			}
				break
			case BOTTOM: if (rect.height >0 ){
				rect.height -= step
				return true
			}
		}

		return false;
	}

	// return array of overlaps on each side of the rectangle (LEFT, RIGHT, etc.)
	// for instance: overlap of 15 pixels on the left side means that rectangle should be shortened by
	//   15 pixels from its right side to fit between neighbours
	// index 0 in the array is a sum of all overlaps (to know when to stop adjusting)
	// return example: [16, 0, 15, 1, 0] - 15 pixels on the right and 1 pixel on the bottom
	public static List getSidesOverlaps(rect, neighbours){
		int x = rect.x, y = rect.y, tox = rect.x+rect.width-1, toy = rect.y + rect.height-1
		int left=0, right=0, top=0, bottom=0
		neighbours.each{n->
			int nx = n.x, ny = n.y
			int ntox = n.x + n.width -1
			int ntoy = n.y + n.height -1

			// first need to detect if room is inside this cell (a special case not covered by code below)
			// TODO

			// no we can check overlap
			// check left side of other room
			if ( nx >= x && nx < tox) { // potential overlap on the right
				if (between(y, ny, ntoy-1) || between(toy, ny+1, ntoy) || between(ny, y, toy-1) || between(ntoy, y+1, toy))
					right = Math.max(right, tox - nx)
			}

			if (ntox > x && ntox<=tox) { // potential overlap on the left
				if (between(y, ny, ntoy-1) || between(toy, ny+1, ntoy) || between(ny, y, toy-1) || between(ntoy, y+1, toy))
					left = Math.max(left, ntox - x)
			}

			if (ntoy> y && ntoy<=toy) { // potential overlap on the top
				if (between(x, nx, ntox-1) || between(tox, nx+1, ntox) || between(nx, x, tox-1) || between(ntox, x+1, tox))
					top = Math.max(top, ntoy - y)
			}

			if (ny >= y && ny<toy) { // potential overlap on the bottom
				if (between(x, nx, ntox-1) || between(tox, nx+1, ntox) || between(nx, x, tox-1) || between(ntox, x+1, tox))
					bottom = Math.max(bottom, toy - ny)
			}
		}
		return [left+right+top+bottom, top, right, bottom, left]
	}

	// get boundaries of the rectangle (cell) at x,y in the grid* to fit between surrounding rectangles
	// * grid contains rectangles, where each rectangle has coordinates on some map
	// this is simplified method which doesnt do a good job
	@Deprecated
	public static Map getCellBoundaries (int x, int y, grid){

		int maxCellx = grid.size() - 1
		int maxCelly = grid[0].size()-1

		// returns cell, or null if its out of grid boundaries or not placed yet
		def getCellSafe = {p ->
			def otherX = x + p.x, otherY = y + p.y
			if (otherX >= 0 && otherX <= maxCellx && otherY >= 0 && otherY <= maxCelly){
				def cell = grid[otherX][otherY]
				if (cell.width>0 && cell.height>0)
					return cell
				else
					return null
			}else
				return null
		}

		def left = 0, right = 300000, top = 0, bottom = 300000

		// get rooms on the left (if there are any) and take the biggest x. that is our left boundary
		def cells = dirLeft.collect(getCellSafe).findAll {return it}
		if (cells.size()>0) left = cells.collect{return it.x + it.width}.max()

		// get rooms on the right and get minimum x, thats our right boundary
		cells = dirRight.collect(getCellSafe).findAll {return it}
		if (cells.size()>0) right = (cells*.x).min()

		cells = dirUp.collect(getCellSafe).findAll {return it}
		if (cells.size()>0) top = cells.collect{return it.y + it.height}.max()

		cells = dirDown.collect(getCellSafe).findAll {return it}
		if (cells.size()>0) bottom = (cells*.y).min()

		return [left: left, right: right, top: top, bottom: bottom]
	}

	// returns number indicating which wall is bordering with another cell (top, right, bottom or left)
	// same for the other wall
	// this method assumes that the other cell is directly on the right/left or top/bottom.
	//  will fail on diagonal cell positions
	@Deprecated
	public static Map getBorderingWalls (rect, otherRect){
		def wall, otherWall

		int centerX = rect.x +  Math.round(Math.floor(rect.width / 2))
		int centerY = rect.y +  Math.round(Math.floor(rect.height / 2))
		int otherCenterX = otherRect.x +  Math.round(Math.floor(otherRect.width / 2))
		int otherCenterY = otherRect.y +  Math.round(Math.floor(otherRect.height / 2))

		def m = otherCenterY - centerY
		def n = otherCenterX - centerX

		// half width/height of our room. these are always positive
		def dy = centerY - rect.y
		def dx = centerX - rect.x

		def directionRatio = Math.abs(((double)m)/n) // angle of line connecting centers
		def roomRatio = ((double)dy)/dx    // angle of diagonal

		if (directionRatio > roomRatio){
			// its top or bottom
			wall = m < 0? 1 : 3
		}else{
			// its left or right
			wall = n > 0? 2: 4
		}

		// same for other room
		dy = otherCenterY - otherRect.y
		dx = otherCenterX - otherRect.x
		roomRatio = ((double)dy)/dx
		if (directionRatio > roomRatio){
			// its top or bottom
			otherWall = m < 0? 1 : 3
		}else{
			// its left or right
			otherWall = n > 0? 2: 4
		}
		return [wall: wall, otherWall: otherWall]
	}

	// creates a door in the wall (random position in given direction)
	//   and returns a point outside of the room to connect a corridor to it
	@Deprecated
	public static List getWallPosition (aRoom, aDirection, map){
		def rx, ry, door

		if(aDirection == 1 || aDirection == 3){
			rx = randomInt(aRoom.x + 1, aRoom.x + aRoom.width - 2);
			if(aDirection == 1){
				ry = aRoom.y - 2;
				door = ry + 1;
			}else{
				ry = aRoom.y + aRoom.height + 1;
				door = ry -1;
			}

			map[rx][door] = 0;
		}else if(aDirection == 2 || aDirection == 4){
			ry = randomInt(aRoom.y + 1, aRoom.y + aRoom.height - 2);
			if(aDirection == 2){
				rx = aRoom.x + aRoom.width + 1;
				door = rx - 1;
			}else{
				rx = aRoom.x - 2;
				door = rx + 1;
			}

			map[door][ry] = 0;
		}
		if (rx >= 300 || ry >= 300){
			println "For ($rx,$ry) and Direction $aDirection, x,y=${aRoom.x},${aRoom.y}  w,h=${aRoom.width},${aRoom.height}"
		}

		return [rx, ry];
	}

	// 'draws' a line between two points:
	//  calculates pixels that line occupies, and calls callback function on them
	public static void bresenham (from, to, drawCallback){
		int xOffset = to.x - from.x;
		int yOffset = to.y - from.y;

		int xCount = 0, yCount = 0;

		if(xOffset != 0) xCount = xOffset > 0 ? 1 : -1;
		if(yOffset != 0) yCount = yOffset > 0 ? 1 : -1;

		def x = from.x, y = from.y
		drawCallback(x, y)
		while(xOffset != 0 || yOffset != 0){
			if (Math.abs(xOffset) > Math.abs(yOffset)){
				// move horizontally
				x += xCount
				xOffset -= xCount
			}else{
				// move vertically
				y += yCount
				yOffset -= yCount
			}
			if (x>300 || y>300 || x<0 || y<0)
				println("Oops $x, $y for [${from.x},${from.y}] - [${to.x},${to.y}]")
			drawCallback(x,y)
		}
	}


}