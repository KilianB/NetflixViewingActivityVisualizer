package com.github.kilianB.fileHandling.out;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Rudimentary synchronized csv writer  
 * @author Kilian
 *
 */
public class CSVWriter implements AutoCloseable {

	private static final Logger LOGGER = Logger.getLogger(CSVWriter.class.getName());

	/**
	 * Writer used to write to the file
	 */
	protected final BufferedWriter bw;

	/**
	 * Delimiter used to seperate values
	 */
	protected String delimiter = ";";
	
	/**
	 * Lock to keep write access synchronized
	 */
	protected ReentrantLock writeLock = new ReentrantLock();
	
	/**
	 * Count of headers. R requires consistent header and data length
	 */
	private int headerLength = 0;
	
	public CSVWriter(File csvOutPath, String delimiter,String... headers) throws IOException {
		this.bw = new BufferedWriter(new FileWriter(csvOutPath));
		this.delimiter = delimiter;
		writeHeader(headers);
	}

	/**
	 * Write a header row at the beginning of a field
	 * @param headers The header title fields
	 * @throws IOException if an IO error occurs
	 */
	private void writeHeader(String... headers) throws IOException {
		
		headerLength = headers.length;
		
		StringBuilder haederBuilder = new StringBuilder();

		Iterator<String> iter = Arrays.asList(headers).iterator();

		while (iter.hasNext()) {
			haederBuilder.append(iter.next());
			if (iter.hasNext())
				haederBuilder.append(delimiter);
		}
		haederBuilder.append(System.lineSeparator());

		lockedWrite(haederBuilder.toString());
		
	}

	/**
	 * Append the supplied content to the file. 
	 * This method synchronized write actions.
	 * @param content
	 * @throws IOException
	 */
	protected void lockedWrite(String content) throws IOException{
		writeLock.lock();
		try {
			bw.write(content);
		}catch(IOException io) {
			//Retrow
			throw io;
		}finally {
			writeLock.unlock();
		}
	}
	
	
	/**
	 * Append the objects to the end of the file. Each object
	 * will be treated as a new value seperated by a delimiter. 
	 * 
	 * This action is synchronized
	 * @param entries entries to be appended to the file
	 * @throws IOException
	 */
	public void writeLine(Object...entries) throws IOException {
		
		if(entries.length > headerLength) {
			LOGGER.warning("More entries added than header fields written");
		}
		
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < entries.length; i++) {
			sb.append(entries[i]);
			if(i < entries.length -1) {
				sb.append(delimiter);
			}			
		}
		
		sb.append(System.lineSeparator());
		lockedWrite(sb.toString());
	}
	
	
	@Override
	public void close(){
		try {
			bw.close();
		}catch(IOException io) {
			io.printStackTrace();
		}
	}
}



