package net.simpleframework.workflow.engine;

import net.simpleframework.ctx.ado.IADOModuleContext;
import net.simpleframework.workflow.engine.participant.IParticipantModel;
import net.simpleframework.workflow.engine.remote.IProcessRemote;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IWorkflowContext extends IADOModuleContext {

	/**
	 * 模型管理器
	 * 
	 * @return
	 */
	IProcessModelManager getModelMgr();

	/**
	 * 流程实例管理器
	 * 
	 * @return
	 */
	IProcessManager getProcessMgr();

	/**
	 * 任务环节管理器
	 * 
	 * @return
	 */
	IActivityManager getActivityMgr();

	/**
	 * 工作列表管理器
	 * 
	 * @return
	 */
	IWorkitemManager getWorkitemMgr();

	/**
	 * 参与者模型接口
	 * 
	 * @return
	 */
	IParticipantModel getParticipantMgr();

	IProcessRemote getRemoteMgr();
}
