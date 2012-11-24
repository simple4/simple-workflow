package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;
import net.simpleframework.ado.db.common.DbTable;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.common.ado.query.IDataQuery;
import net.simpleframework.common.task.ExecutorRunnable;
import net.simpleframework.common.task.TaskExecutor;
import net.simpleframework.ctx.Module;
import net.simpleframework.ctx.ado.AbstractADOModuleContext;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityLobBean;
import net.simpleframework.workflow.engine.DelegationBean;
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.IActivityManager;
import net.simpleframework.workflow.engine.IProcessManager;
import net.simpleframework.workflow.engine.IProcessModelManager;
import net.simpleframework.workflow.engine.IWorkflowContext;
import net.simpleframework.workflow.engine.IWorkitemManager;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.ProcessLobBean;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.ProcessModelLobBean;
import net.simpleframework.workflow.engine.VariableBean;
import net.simpleframework.workflow.engine.VariableLogBean;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.participant.IParticipantModel;
import net.simpleframework.workflow.engine.remote.DefaultProcessRemote;
import net.simpleframework.workflow.engine.remote.IProcessRemote;
import net.simpleframework.workflow.schema.AbstractTaskNode;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class WorkflowContext extends AbstractADOModuleContext implements IWorkflowContext {

	@Override
	public void onInit() throws Exception {
		super.onInit();
		dataServiceFactory
				.putEntityService(ProcessModelBean.class, new DbTable("sf_workflow_model"))
				.putEntityService(ProcessModelLobBean.class, new DbTable("sf_workflow_model_lob", true))
				.putEntityService(ProcessBean.class, new DbTable("sf_workflow_process"))
				.putEntityService(ProcessLobBean.class, new DbTable("sf_workflow_process_lob", true))
				.putEntityService(DelegationBean.class, new DbTable("sf_workflow_delegation"))
				.putEntityService(ActivityBean.class, new DbTable("sf_workflow_activity"))
				.putEntityService(ActivityLobBean.class, new DbTable("sf_workflow_activity_lob", true))
				.putEntityService(WorkitemBean.class, new DbTable("sf_workflow_workitem"))
				.putEntityService(VariableBean.class, new DbTable("sf_workflow_variable"))
				.putEntityService(VariableLogBean.class, new DbTable("sf_workflow_variable_log"));

		final TaskExecutor taskExecutor = new TaskExecutor();
		// 引擎的初始化
		taskExecutor.execute(new ExecutorRunnable() {
			@Override
			protected void task() throws Exception {
				// 启动子流程监控
				final IDataQuery<ActivityBean> qs = getEntityService(ActivityBean.class).query(
						new ExpressionValue("tasknodeType=? and (status=? or status=?)",
								AbstractTaskNode.SUBNODE_TYPE, EActivityStatus.running,
								EActivityStatus.waiting), ActivityBean.class);
				final IActivityManager activityMgr = getActivityMgr();
				ActivityBean activity;
				while ((activity = qs.next()) != null) {
					activityMgr.doRemoteSubTask(activity);
				}
			}
		});
	}

	@Override
	protected Module createModule() {
		return new Module().setName("simple-workflow").setText($m("WorkflowContext.0")).setOrder(2);
	}

	@Override
	public IProcessModelManager getModelMgr() {
		return singleton(ProcessModelManager.class);
	}

	@Override
	public IProcessManager getProcessMgr() {
		return singleton(ProcessManager.class);
	}

	@Override
	public IActivityManager getActivityMgr() {
		return singleton(ActivityManager.class);
	}

	@Override
	public IWorkitemManager getWorkitemMgr() {
		return singleton(WorkitemManager.class);
	}

	@Override
	public IProcessRemote getRemoteMgr() {
		return singleton(DefaultProcessRemote.class);
	}

	@Override
	public IParticipantModel getParticipantMgr() {
		try {
			return (IParticipantModel) singleton("net.simpleframework.workflow.web.DefaultParticipantModel");
		} catch (final ClassNotFoundException e) {
			throw WorkflowException.of(e);
		}
	}
}
