package org.deffer.utils2d

import javax.imageio.ImageIO
import java.awt.image.BufferedImage


class ImageUtil {
	// TILE_TERRAIN, TILE_EMPTY, TILE_WALL,TILE_PATH,TILE_CELL
	static int[] COLORS_MAP = [0xFFDDDDDD, 0xFF999999, 0xFF444444, 0xFF44DD44, 0xFFAA1111]

	// filename without extension
	public static void writeMap(String filename, map, int sizex, int sizey){
		BufferedImage bi = new BufferedImage(sizex, sizey, BufferedImage.TYPE_INT_ARGB);
		int[] result = new int[sizex*sizey]
		(0..(sizey-1)).each{int y->
			(0..(sizex-1)).each{int x->
				// rgbArray[offset + (y-startY)*scansize + (x-startX)];
				// for startY = 0, startX=0, offset=0...
				result[y*sizex + x] = COLORS_MAP[map[x][y]]
			}
		}
		bi.setRGB(0, 0, sizex, sizey, result, 0, sizex);

		File file = new File(filename+".png")
		ImageIO.write(bi, "png", file)
	}

	public static void writeMapZoomX(String filename, map, int sizex, int sizey, int zoom){
		BufferedImage bi = new BufferedImage(sizex*zoom, sizey*zoom, BufferedImage.TYPE_INT_ARGB);
		//int[] result = new int[sizex*sizey]
		(0..(sizey-1)).each{int y->
			int[] result = new int[sizex*zoom]
			// generate one horizontal line of image
			(0..(sizex-1)).each{int x->
				(0..zoom-1).each{i->
					result[x*zoom+i] = COLORS_MAP[map[x][y]]
				}
			}

			(0..zoom-1).each {i->
				int imageY = y*zoom+i
				bi.setRGB(0, imageY, sizex*zoom, 1, result, 0, sizex*zoom);
			}
		}


		File file = new File(filename+".png")
		ImageIO.write(bi, "png", file)
	}

	public static void main(String[] args){

		// TYPE_INT_ARGB specifies the image format: 8-bit RGBA packed
		// into integer pixels
		int[] pixels = new int[80*80]
		(0..79).each{x->
			(0..79).each{y->
				if ((x % 20) == 0) {
					pixels[x*80+y] = 0xFFFF0000
				}else{
					pixels[x*80+y] = 0xFF141444
				}
			}
		}

		// type TYPE_INT_ARGB doesnt work with bmp
		// "You might have to work on the encoding and transform your image to the desired format."
		// see:  http://stackoverflow.com/questions/18956941/imageio-write-bmp-does-not-work
		BufferedImage bi = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		bi.setRGB(0, 0, 80, 80, pixels, 0, 80);

		File file = new File("g:\\dev\\img.png")
		ImageIO.write(bi, "png", file) // assert true
	}
}
