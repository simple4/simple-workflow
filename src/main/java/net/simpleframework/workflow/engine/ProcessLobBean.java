package net.simpleframework.workflow.engine;

import net.simpleframework.common.bean.AbstractIdBean;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ProcessLobBean extends AbstractIdBean {
	private char[] processModel;

	public char[] getProcessModel() {
		return processModel;
	}

	public void setProcessModel(final char[] processModel) {
		this.processModel = processModel;
	}

	private static final long serialVersionUID = 219135045162448573L;
}
