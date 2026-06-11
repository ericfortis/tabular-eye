package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class SqlDetectorTest extends BasePlatformTestCase {
	private SqlDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new SqlDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("test.sql", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testBasicCreateTable() {
		var blocks = getBlocks("""
			 CREATE TABLE users (
			  id UUID PRIMARY KEY,
			  email TEXT NOT NULL UNIQUE,
			  salt TEXT,
			  hash TEXT,
			  created_at TIMESTAMPTZ DEFAULT NOW());
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(5, block.size());

		assertEquals("id", block.get(0).key());
		assertEquals("email", block.get(1).key());
		assertEquals("salt", block.get(2).key());
		assertEquals("hash", block.get(3).key());
		assertEquals("created_at", block.get(4).key());
	}

	public void testMultipleCreateTables() {
		var blocks = getBlocks("""
			 CREATE TABLE users (
			  id UUID PRIMARY KEY,
			  email TEXT NOT NULL UNIQUE
			 );
			 CREATE TABLE files (
			  id UUID PRIMARY KEY,
			  title TEXT,
			  saved_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
			 );
			 """);
		assertEquals(2, blocks.size());

		assertEquals(2, blocks.get(0).size());
		assertEquals("id", blocks.get(0).get(0).key());
		assertEquals("email", blocks.get(0).get(1).key());

		assertEquals(3, blocks.get(1).size());
		assertEquals("id", blocks.get(1).get(0).key());
		assertEquals("title", blocks.get(1).get(1).key());
		assertEquals("saved_at", blocks.get(1).get(2).key());
	}

	public void testIgnoresSingleLineCreateTable() {
		var blocks = getBlocks("""
			 CREATE TABLE users (id UUID, name TEXT);
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testIgnoresSingleColumnTable() {
		var blocks = getBlocks("""
			 CREATE TABLE users (
			  id UUID PRIMARY KEY
			 );
			 """);
		assertTrue(blocks.isEmpty());
	}
}
