package com.ericfortis.tabulareye.detectors;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class HtmlDetectorTest extends BasePlatformTestCase {
	private JsObjectLiteralDetector jsDetector;
	private Js2DArrayDetector js2dDetector;
	private CssPropertyDetector cssDetector;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		jsDetector = new JsObjectLiteralDetector();
		js2dDetector = new Js2DArrayDetector();
		cssDetector = new CssPropertyDetector();
	}

	public void testJsObjectInScriptTag() {
		var file = myFixture.configureByText("test.html", """
			 <!DOCTYPE html>
			 <html>
			 <head>
			 <script>
			 const obj = {
			 	foo: 1,
			 	bar: 2
			 };
			 </script>
			 </head>
			 </html>
			 """);
		var blocks = jsDetector.findBlocks(file, myFixture.getDocument(file));
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.getFirst().size());
		assertEquals("foo", blocks.getFirst().get(0).key());
		assertEquals("bar", blocks.getFirst().get(1).key());
	}

	public void testJs2DArrayInScriptTag() {
		var file = myFixture.configureByText("test.html", """
			 <!DOCTYPE html>
			 <html>
			 <head>
			 <script>
			 const arr = [
			 	[1, 2],
			 	[3, 4]
			 ];
			 </script>
			 </head>
			 </html>
			 """);
		var blocks = js2dDetector.findBlocks(file, myFixture.getDocument(file));
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.getFirst().size());
	}

	public void testCssInStyleTag() {
		var file = myFixture.configureByText("test.html", """
			 <!DOCTYPE html>
			 <html>
			 <head>
			 <style>
			 .container {
			 	color: red;
			 	background: blue;
			 }
			 </style>
			 </head>
			 </html>
			 """);
		var blocks = cssDetector.findBlocks(file, myFixture.getDocument(file));
		assertEquals(1, blocks.size());
		assertEquals(2, blocks.getFirst().size());
		assertEquals("color", blocks.getFirst().get(0).key());
		assertEquals("background", blocks.getFirst().get(1).key());
	}
}
