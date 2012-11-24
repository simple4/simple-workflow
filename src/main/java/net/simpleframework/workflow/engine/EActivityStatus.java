package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public enum EActivityStatus {

	running {

		@Override
		public String toString() {
			return $m("EActivityStatus.running");
		}
	},

	complete {

		@Override
		public String toString() {
			return $m("EActivityStatus.complete");
		}
	},

	suspended {

		@Override
		public String toString() {
			return $m("EActivityStatus.suspended");
		}
	},

	abort {

		@Override
		public String toString() {
			return $m("EActivityStatus.abort");
		}
	},

	timeout {

		@Override
		public String toString() {
			return $m("EActivityStatus.timeout");
		}
	},

	waiting {

		@Override
		public String toString() {
			return $m("EActivityStatus.waiting");
		}
	}
}
