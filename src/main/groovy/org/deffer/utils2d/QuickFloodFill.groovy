package org.deffer.utils2d;


class QuickFloodFill {
	public static long run(map, int startx, int starty, fill){
		def probe = map[startx][starty]
		def getClosure = this.&getColor.curry(map, -1)
		def setClosure = this.&setColor.curry(map)

		return floodFill(startx, starty, probe, fill, getClosure, setClosure)
	}

	public static long floodFill(int x, int y, probe, fill, getColor, setColor) {

		if (getColor(x,y) != probe) return;

		Queue queue = new LinkedList()
		queue.add([x: x, y: y])
		long pixelCount = 0;
		while (!queue.isEmpty()){
			def point = queue.remove()
			if (getColor(point.x, point.y) == probe){
				setColor(point.x, point.y, fill)
				pixelCount ++

				queue.add([x: point.x + 1, y: point.y]);
				queue.add([x: point.x - 1, y: point.y]);
				queue.add([x: point.x, y: point.y + 1]);
				queue.add([x: point.x, y: point.y - 1]);
			}
		}

		return pixelCount
	}

	public static def getColor(map, incorrectColorFlag, int x, int y){
		if (map.size() >= x)
			return incorrectColorFlag;

		def column = map[x]
		if (column.size() >= y)
			return incorrectColorFlag

		return column[y]
	}

	public static def setColor(map, int x, int y, color){
		if (map.size() >= x) return

		def column = map[x]
		if (column.size() >= y) return

		column[y] = color
	}

	@Deprecated
	public static void floodFillRecursive(int x, int y, probe, fill, getColor, setColor) {

		if (getColor(x,y) != probe) return;

		setColor(x, y)
		floodFillRecursive(x - 1, y, probe, fill, getColor, setColor);
		floodFillRecursive(x + 1, y, probe, fill, getColor, setColor);
		floodFillRecursive(x, y - 1, probe, fill, getColor, setColor);
		floodFillRecursive(x, y + 1, probe, fill, getColor, setColor);
	}
}
