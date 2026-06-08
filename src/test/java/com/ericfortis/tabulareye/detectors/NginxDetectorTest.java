package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class NginxDetectorTest extends BasePlatformTestCase {
	private NginxDetector detector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		detector = new NginxDetector();
	}

	private @NonNull List<AlignmentDetector.AlignmentBlock> getBlocks(String content) {
		var file = myFixture.configureByText("nginx.conf", content);
		var doc = myFixture.getDocument(file);
		return detector.findBlocks(file, doc);
	}

	public void testIsApplicable() {
		var file = myFixture.configureByText("nginx.conf", "etag off;");
		assertTrue(detector.isApplicable(file));
	}

	public void testIsApplicableWithPrefix() {
		var file = myFixture.configureByText("my-nginx.conf", "etag off;");
		assertTrue(detector.isApplicable(file));
	}

	public void testIsNotApplicableForOtherFiles() {
		var file = myFixture.configureByText("apache.conf", "etag off;");
		assertFalse(detector.isApplicable(file));
	}

	public void testBasicDirectives() {
		var blocks = getBlocks("""
			 etag off;
			 server_tokens off;
			 return 301 https://$host$request_uri;
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(3, block.size());

		assertEquals("etag", block.get(0).key());
		assertEquals("server_tokens", block.get(1).key());
		assertEquals("return", block.get(2).key());
	}

	public void testIgnoresBlockDirectivesWithBraces() {
		var blocks = getBlocks("""
			 server {
			   listen 80;
			   server_name example.com;
			 }
			 """);
		assertEquals(1, blocks.size());

		var block = blocks.getFirst();
		assertEquals(2, block.size());
		assertEquals("listen", block.get(0).key());
		assertEquals("server_name", block.get(1).key());
	}

	public void testEmptyLineSeparatesBlocks() {
		var blocks = getBlocks("""
			 etag off;
			 server_tokens off;
			 
			 proxy_pass http://backend;
			 proxy_set_header Host $host;
			 """);
		assertEquals(2, blocks.size());

		assertEquals(2, blocks.get(0).size());
		assertEquals("etag", blocks.get(0).get(0).key());
		assertEquals("server_tokens", blocks.get(0).get(1).key());

		assertEquals(2, blocks.get(1).size());
		assertEquals("proxy_pass", blocks.get(1).get(0).key());
		assertEquals("proxy_set_header", blocks.get(1).get(1).key());
	}

	public void testBraceLinesBreakBlocks() {
		var blocks = getBlocks("""
			 worker_connections 1024;
			 use epoll;
			 location / {
			   proxy_pass http://backend;
			   proxy_set_header Host $host;
			 }
			 """);
		assertEquals(2, blocks.size());
		assertEquals("worker_connections", blocks.get(0).get(0).key());
		assertEquals("use", blocks.get(0).get(1).key());
		assertEquals("proxy_pass", blocks.get(1).get(0).key());
		assertEquals("proxy_set_header", blocks.get(1).get(1).key());
	}

	public void testSingleDirectiveIsNotABlock() {
		var blocks = getBlocks("""
			 etag off;
			 """);
		assertTrue(blocks.isEmpty());
	}

	public void testCommentLineSeparatesBlocks() {
		var blocks = getBlocks("""
			 etag off;
			 server_tokens off;
			 # comments are ignored
			 proxy_pass http://backend;
			 proxy_set_header Host $host;
			 """);
		assertEquals(2, blocks.size());

		assertEquals(2, blocks.get(0).size());
		assertEquals("etag", blocks.get(0).get(0).key());
		assertEquals("server_tokens", blocks.get(0).get(1).key());

		assertEquals(2, blocks.get(1).size());
		assertEquals("proxy_pass", blocks.get(1).get(0).key());
		assertEquals("proxy_set_header", blocks.get(1).get(1).key());
	}
}
