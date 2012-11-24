package net.simpleframework.workflow.engine.remote;

import java.io.IOException;
import java.util.Map;

import net.simpleframework.common.web.HttpClient;
import net.simpleframework.workflow.remote.AbstractWorkflowRemote;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */

public class DefaultProcessRemote extends AbstractWorkflowRemote implements IProcessRemote {

	// from config
	private final String remote_page = "/sf/workflow-web-remote-SubProcessRemotePage";

	@Override
	public Map<String, Object> call(final String url, final String method,
			final Map<String, Object> data) throws IOException {
		return HttpClient.of(url).post(remote_page + "?method=" + method, data);
	}
}
