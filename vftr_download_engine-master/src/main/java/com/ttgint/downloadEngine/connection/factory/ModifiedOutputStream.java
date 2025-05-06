/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.factory;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 *
 * @author TTGETERZI
 */
public class ModifiedOutputStream extends FileOutputStream {

    private final ArrayList<Character.UnicodeBlock> validCharacterUnicode
            = new ArrayList<>();

    public ModifiedOutputStream(String name) throws FileNotFoundException   {
        super(name);
    }

    public ModifiedOutputStream(String name, boolean append) throws FileNotFoundException {
        super(name, append);
    }

    public ModifiedOutputStream(File file) throws FileNotFoundException {
        super(file);
    }

    public ModifiedOutputStream(File file, boolean append) throws FileNotFoundException {
        super(file, append);
    }

    public ModifiedOutputStream(FileDescriptor fdObj) {
        super(fdObj);
    }

    @Override
    public FileChannel getChannel() {
        return super.getChannel();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    public void checkDiscardedAndWrite(byte[] b, int off, int len) throws IOException {
        int tempoff = off;
        int newLenght = 0;
        byte[] newByte = new byte[len];
        System.out.println(new String(b, off, len));
        for (; off < len; off++) {
            char charh = (char) b[off];
            if (discardCharset(charh)) {
                newByte[newLenght++] = b[off];
            }
        }
        String str = new String(newByte,
                tempoff, newLenght);
        write(str.getBytes("UTF-8"), tempoff, newLenght);
    }

    public void checkXmlAndWrite(byte[] b, int off, int len) throws IOException {
        int tempoff = off;
        int newLenght = 0;
        byte[] newByte = new byte[len];
        for (; off < len; off++) {
            char charh = (char) b[off];
            if (discardNonValidXmlCharacters(charh)) {
                
                if (validCharacterUnicode.isEmpty() == false) {
                    if (discardCharset(charh)) {
                        newByte[newLenght++] = b[off];
                    }
                } else {
                    newByte[newLenght++] = b[off];
                }
            }
        }
        String str = new String(newByte,
                tempoff, newLenght);

        super.write(str.getBytes("UTF-8"), tempoff, newLenght);
    }

    @Override
    public void write(byte[] b) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(int b) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() throws IOException {
        super.flush();
    }

    public boolean discardNonValidXmlCharacters(char c) {
        return (c == 0x9)
                || (c == 0xA)
                || (c == 0xD)
                || ((c >= 0x20) && (c <= 0xD7FF))
                || ((c >= 0xE000) && (c <= 0xFFFD))
                || ((c >= 0x10000) && (c <= 0x10FFFF));
    }

    public boolean discardCharset(char args) {
        boolean status = true;
        for (Character.UnicodeBlock each : validCharacterUnicode) {
            if (Character.UnicodeBlock.of(args) != each) {
                status = false;
                break;
            }
        }
        return status;
    }

    public void addDiscardedCharset(Character.UnicodeBlock unicode) {
        validCharacterUnicode.add(unicode);
    }

}
