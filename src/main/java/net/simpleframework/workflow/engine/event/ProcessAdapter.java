package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.EProcessAbortPolicy;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.ProcessBean;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ProcessAdapter implements IProcessListener {

	@Override
	public void onProcessCreated(final InitiateItem initiateItem, final ProcessBean process) {
	}

	@Override
	public void onAbort(final ProcessBean process, final EProcessAbortPolicy policy) {
	}

	@Override
	public void onSuspend(final ProcessBean process) {
	}
}
