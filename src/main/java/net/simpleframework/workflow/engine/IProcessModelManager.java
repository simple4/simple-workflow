package net.simpleframework.workflow.engine;

import net.simpleframework.common.ID;
import net.simpleframework.common.ado.query.IDataQuery;
import net.simpleframework.ctx.ado.IBeanManagerAware;
import net.simpleframework.workflow.schema.ProcessDocument;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IProcessModelManager extends IBeanManagerAware<ProcessModelBean>,
		IListenerAware<ProcessModelBean>, IScriptAware<ProcessModelBean> {

	/**
	 * 获取模型的文档对象
	 * 
	 * @param processModel
	 * @return
	 */
	ProcessDocument getProcessDocument(ProcessModelBean processModel);

	ProcessModelBean addModel(ID userId, ProcessDocument processDocument);

	void updateModel(ProcessModelBean processModel, ID userId, char[] model);

	/**
	 * 根据名称获取ProcessModelBean实例
	 * 
	 * @param name
	 * @return
	 */
	ProcessModelBean getProcessModelByName(String name);

	/**
	 * 根据名字或则id获取模型，如果不存在，则抛异常
	 * 
	 * @param model
	 *           名字或则id
	 * @return
	 */
	ProcessModelBean getProcessModel(String model);

	/**
	 * 
	 * @param status
	 * @return
	 */
	IDataQuery<ProcessModelBean> getModelList(EProcessModelStatus... status);

	/**
	 * 
	 * @param userId
	 * @return
	 */
	InitiateItems getInitiateItems(ID userId);

	/**
	 * 判断用户是否可以启动指定的流程模型
	 * 
	 * @param userId
	 * @param model
	 *           实例或者是id
	 * @return
	 */
	boolean isStartProcess(ID userId, Object model);

	/**
	 * 设置模型的状态
	 * 
	 * @param processModel
	 * @param status
	 */
	void setStatus(ProcessModelBean processModel, EProcessModelStatus status);
}
