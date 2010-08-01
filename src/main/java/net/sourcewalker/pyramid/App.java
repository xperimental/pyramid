package net.sourcewalker.pyramid;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class App implements Runnable {

    private String inputFile;
    private int tileSize;
    private String outputPath;
    private boolean specialZero;

    public App(String[] args) {
        if (args.length == 1) {
            inputFile = args[0];
            outputPath = inputFile + "-pyramid" + File.separator;
        }
        tileSize = 512;
    }

    public void run() {
        System.out.println("Simple Image Pyramid Creator\n");
        if (inputFile == null) {
            usage();
            return;
        }
        System.out.println("      input: " + inputFile);
        System.out.println("output path: " + outputPath);
        System.out.println("  tile size: " + tileSize);
        System.out.println("loading input...");
        Image original = loadImage(inputFile);
        int inputWidth = original.getWidth(null);
        int inputHeight = original.getHeight(null);
        System.out.println(" input size: " + inputWidth + " x " + inputHeight
                + " pixel");
        checkValidSize(inputWidth, inputHeight);
        int xTiles = inputWidth / tileSize;
        int yTiles = inputHeight / tileSize;
        int levels = (int) binLog(Math.min(inputWidth, inputHeight) / tileSize) + 1;
        if (xTiles != yTiles) {
            levels++;
            specialZero = true;
        }
        System.out.println("# of levels: " + levels + "\n");
        for (int i = levels - 1; i > -1; i--) {
            System.out.println("render level " + i + "...");
            if (i == 0 && specialZero) {
                System.out.println("creating level 0 preview image...");
                BufferedImage tile = new BufferedImage(tileSize, tileSize,
                        BufferedImage.TYPE_INT_ARGB);
                if (xTiles > 0) {
                    int count = 2 * xTiles;
                    for (int y = 0; y < count; y++) {
                        int y1 = y * tileSize / count;
                        int y2 = (y + 1) * tileSize / count;
                        tile.getGraphics().drawImage(original, 0, y1, tileSize,
                                y2, 0, 0, inputWidth, inputHeight, null);
                    }
                } else if (yTiles > 0) {
                    int count = 2 * yTiles;
                    for (int x = 0; x < count; x++) {
                        int x1 = x * tileSize / count;
                        int x2 = (x + 1) * tileSize / count;
                        tile.getGraphics().drawImage(original, x1, 0, x2,
                                tileSize, 0, 0, inputWidth, inputHeight, null);
                    }
                } else {
                    throw new RuntimeException(
                            "Error, one should be greater than 0 at this point!");
                }
                writeTile(0, 0, 0, tile);
            } else {
                int xInput = inputWidth / xTiles;
                int yInput = inputHeight / yTiles;
                int max = xTiles * yTiles;
                int count = 0;
                System.out.println("    num tiles: " + xTiles + " x " + yTiles
                        + " (" + max + ")");
                System.out.println(" input pixels: " + xInput + " x " + yInput);
                for (int y = 0; y < yTiles; y++) {
                    int top = y * yInput;
                    int bottom = top + yInput;
                    for (int x = 0; x < xTiles; x++) {
                        int left = x * xInput;
                        int right = left + xInput;
                        System.out.print(String.format(" tile %d, %d, %d", i,
                                x, y));
                        BufferedImage tile = new BufferedImage(tileSize,
                                tileSize, BufferedImage.TYPE_INT_ARGB);
                        tile.getGraphics().drawImage(original, 0, 0, tileSize,
                                tileSize, left, top, right, bottom, null);
                        writeTile(i, x, y, tile);
                        System.out.println(" ok. ("
                                + (int) ((double) count / max * 100) + "%)");
                        count++;
                    }
                    System.gc();
                }
                System.out.println();
                xTiles /= 2;
                yTiles /= 2;
            }
        }
        System.out.println("done.");
    }

    private void writeTile(int level, int x, int y, BufferedImage tile) {
        File outputFile = createOutputFile(level, x, y);
        try {
            System.out.print(" writing...");
            FileOutputStream out = new FileOutputStream(outputFile);
            ImageIO.write(tile, "png", out);
            out.close();
        } catch (IOException e) {
            System.out.println("Error while writing output: " + e.getMessage());
        }
    }

    private File createOutputFile(int level, int x, int y) {
        String fileDir = outputPath + level + File.separator + y
                + File.separator;
        File outDir = new File(fileDir);
        if (outDir.exists() && (!outDir.isDirectory())) {
            throw new IllegalArgumentException("Output directory is a file: "
                    + outDir.getAbsolutePath());
        }
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        String fileName = fileDir + x + ".png";
        return new File(fileName);
    }

    private double binLog(double value) {
        return Math.log(value) / Math.log(2);
    }

    private void checkValidSize(int width, int height) {
        if (!(isDivisible(width, tileSize) && isDivisible(height, tileSize))) {
            throw new IllegalArgumentException(
                    "Image size is not divisible by tile size.");
        }
    }

    private boolean isDivisible(int value, int divisor) {
        double dFrac = (double) value / divisor;
        return (int) dFrac == dFrac;
    }

    private Image loadImage(String fileName) {
        Image result = null;
        File file = new File(fileName);
        if (!file.exists()) {
            throw new IllegalArgumentException("Input file does not exist!");
        }
        try {
            result = ImageIO.read(file);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Error while reading input file: " + e.getMessage());
        }
        return result;
    }

    private void usage() {
        System.out.println("  usage: java -jar pyramid.jar [inputFile]\n");
        System.out
                .println("inputFile\tpath to input image to be converted into a image pyramid.");
    }

    public static void main(String[] args) {
        App main = new App(args);
        main.run();
    }

}
