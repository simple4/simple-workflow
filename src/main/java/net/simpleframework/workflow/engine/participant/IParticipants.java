package net.simpleframework.workflow.engine.participant;

import java.util.Collection;

import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.script.IScriptEval;
import net.simpleframework.workflow.engine.IWorkflowContext;
import net.simpleframework.workflow.engine.WorkflowContextFactory;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IParticipants {

	/**
	 * 定义交互环节的参与者
	 * 
	 * @param script
	 * @param variables
	 * @return
	 */
	Collection<Participant> participants(IScriptEval script, KVMap variables);

	public abstract static class AbstractParticipants implements IParticipants {

		public IWorkflowContext getModuleContext() {
			return WorkflowContextFactory.get();
		}
	}
}
