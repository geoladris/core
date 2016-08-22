package org.fao.unredd.jwebclientAnalyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PluginDescriptorTest {

	@Test
	public void canHaveNullName() {
		PluginDescriptor descriptor = new PluginDescriptor();
		assertNull(descriptor.getName());
	}

	@Test
	public void equals() {
		PluginDescriptor p1 = new PluginDescriptor();
		PluginDescriptor p2 = new PluginDescriptor();

		String conf = "{'default-conf' : { m1 : true }, "
				+ "requirejs : { paths : { a : '../jslib/A', "
				+ "b : '../jslib/B' } } }";
		p1.setConfiguration(conf);
		p2.setConfiguration(conf);
		p1.getModules().add("m1");
		p2.getModules().add("m1");
		p1.getStylesheets().add("s1");
		p2.getStylesheets().add("s1");

		assertEquals(p1, p2);
		assertEquals(p1.hashCode(), p2.hashCode());
	}

	@Test
	public void notEquals() {
		PluginDescriptor p1 = new PluginDescriptor();
		PluginDescriptor p2 = new PluginDescriptor();

		String conf = "{'default-conf' : { m1 : true }, "
				+ "requirejs : { paths : { a : '../jslib/A', "
				+ "b : '../jslib/B' } } }";
		p1.setConfiguration(conf);
		p2.setConfiguration(conf);
		p1.getModules().add("m1");
		p2.getStylesheets().add("s1");

		assertNotSame(p1, p2);
		assertNotSame(p1.hashCode(), p2.hashCode());
	}

	@Test
	public void equalsEmptyPlugins() {
		assertEquals(new PluginDescriptor(), new PluginDescriptor());
	}

}
