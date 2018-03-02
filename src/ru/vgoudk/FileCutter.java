package ru.vgoudk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java class to cut large files
 */
public class FileCutter {

    private static final String SIZE_PATTERN = "(\\d+)([BKMG]?)";
    private static final int KILO = 1024;
    private static final int MEGA = KILO * 1024;
    private static final int GIGA = MEGA * 1024;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("File cutter <file> <new size>");
            System.exit(-1);
        }
        Path inFilePath = Paths.get(args[0]);
        if (Files.notExists(inFilePath)) {
            System.out.println("Input file " + args[0] + " doesn't exits");
        }

        Pattern pattern = Pattern.compile(SIZE_PATTERN);
        Matcher matcher = pattern.matcher(args[1]);
        long outSizeBytes;

        if (matcher.find()) {
            outSizeBytes = Long.parseLong(matcher.group(1));
            if (matcher.groupCount() == 3) {
                switch (matcher.group(2).charAt(0)) {
                    case 'B':
                        outSizeBytes *= 1;
                        break;
                    case 'K':
                        outSizeBytes *= KILO;
                        break;
                    case 'M':
                        outSizeBytes *= MEGA;
                        break;
                    case 'G':
                        outSizeBytes *= GIGA;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported size " + args[1]);
                }
            }
        } else {
            System.out.println("Size should be defined as 100, 200K, 300M and so on");
            System.exit(-1);
            //stupid Java compliler
            throw new IllegalArgumentException("Exit fails");
        }

        RandomAccessFile inFile = new RandomAccessFile(args[0], "r");
        FileChannel inChannel = inFile.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(MEGA);

        RandomAccessFile outFile = new RandomAccessFile(appendDifferentiator(args[0],args[1]), "rw");
        FileChannel outChannel = outFile.getChannel();

        long totalBytes = 0;
        int bytesRead;

        while ((bytesRead = inChannel.read(buffer)) > 0) {
            buffer.flip();

            final long needsToRead = outSizeBytes - totalBytes;

            if (needsToRead < bytesRead) {
                //needsToRead will be less than in size here
                byte[] tail = new byte[(int) needsToRead];
                buffer.get(tail);
                buffer.clear();
                buffer.put(tail);
                bytesRead = (int) needsToRead;
                buffer.flip();
            }
            outChannel.write(buffer);
            totalBytes += bytesRead;

            if (totalBytes == outSizeBytes) break;

            buffer.clear();
        }

        inChannel.close();
        inFile.close();
        outChannel.close();
        outFile.close();
    }


    /**
     * Добавляет к имени файла "отличительую часть" (differentiator), сохряняя расширение
     * @param fileName имя файла для изменения
     * @param differentiator стока с "отличительным признаком
     * @return новое имя файла
     */
    public static String appendDifferentiator( String fileName, String differentiator){
        String parts[] = fileName.split("\\.");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(".");
            if (i == parts.length - 1) {
                sb.append(differentiator).append(".");
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }

}
