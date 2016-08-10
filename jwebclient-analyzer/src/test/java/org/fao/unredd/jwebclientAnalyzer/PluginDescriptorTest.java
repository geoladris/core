package org.fao.unredd.jwebclientAnalyzer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PluginDescriptorTest {
	private PluginDescriptor descriptor;

	@Test
	public void mergeIfPropertyTrue() {
		String content = "{ '" + PluginDescriptor.PROP_MERGE_CONF + "': true}";
		this.descriptor = new PluginDescriptor(content);
		assertTrue(this.descriptor.getMergeConf());
	}
	@Test
	public void doNotMergeIfPropertyFalse() {
		String content = "{ '" + PluginDescriptor.PROP_MERGE_CONF + "': false}";
		this.descriptor = new PluginDescriptor(content);
		assertFalse(this.descriptor.getMergeConf());
	}
	@Test
	public void doNotMergeIfPropertyMissing() {
		String content = "{}";
		this.descriptor = new PluginDescriptor(content);
		assertFalse(this.descriptor.getMergeConf());
	}
}
