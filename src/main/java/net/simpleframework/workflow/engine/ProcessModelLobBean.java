package net.simpleframework.workflow.engine;

import java.io.InputStream;

import net.simpleframework.common.bean.AbstractIdBean;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ProcessModelLobBean extends AbstractIdBean {

	private char[] processSchema;

	private InputStream processImage;

	public char[] getProcessSchema() {
		return processSchema;
	}

	public void setProcessSchema(final char[] processSchema) {
		this.processSchema = processSchema;
	}

	public InputStream getProcessImage() {
		return processImage;
	}

	public void setProcessImage(final InputStream processImage) {
		this.processImage = processImage;
	}

	private static final long serialVersionUID = -8281273400041652815L;
}
