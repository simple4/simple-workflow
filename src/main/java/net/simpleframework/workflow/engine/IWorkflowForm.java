package net.simpleframework.workflow.engine;

import java.awt.Dimension;
import java.util.Map;

import net.simpleframework.common.coll.KVMap;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IWorkflowForm {

	String getFormForward();

	/**
	 * 完成当前工作项
	 * 
	 * @param parameters
	 * @param workitemComplete
	 */
	void onComplete(Map<String, String> parameters, WorkitemComplete workitemComplete);

	/**
	 * 给流程变量赋值
	 * 
	 * @param variables
	 */
	void bindVariables(KVMap variables);

	String getTitle();

	Dimension getSize();
}
