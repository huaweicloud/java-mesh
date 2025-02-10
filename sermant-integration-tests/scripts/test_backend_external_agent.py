#
# Copyright (C) 2025-2025 Sermant Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

import requests
import time
import unittest
import os

class TestBackendExternalAgent(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.mode = os.getenv('TEST_MODE', 'default')
        cls.session = requests.session()

    @classmethod
    def tearDownClass(cls):
        cls.session.close()

    hot_plugging_url = "http://127.0.0.1:8910/sermant/publishHotPluggingConfig"
    query_plugin_info_url = "http://127.0.0.1:8910/sermant/getPluginsInfo"
    header = {'content-type': 'application/json'}
    install_external_agent_param = {
        "commandType": "INSTALL-EXTERNAL-AGENT",
        "instanceIds": "",
        "externalAgentName": "OTEL",
        "agentPath": "opentelemetry-javaagent.jar"
    }

    def get_health_instances(self):
        time.sleep(30)
        resp = self.session.get(self.query_plugin_info_url).json()
        health_instances = [item for item in resp if item.get('health') is True]
        print("Health Instances:", health_instances)
        return health_instances

    def assert_otel_installed(self, instance):
        self.assertIn("OTEL", instance['externalAgentInfoMap'], "OTEL not found in externalAgentInfoMap")
        self.assertEqual("OTEL", instance['externalAgentInfoMap']['OTEL']['name'])
        self.assertEqual("2.10.0", instance['externalAgentInfoMap']['OTEL']['version'])

    def test_external_agent_function(self):
        health_instances = self.get_health_instances()

        if self.mode == 'startWithoutExternalAgent':
            self.assertEqual({}, health_instances[0]['externalAgentInfoMap'])

        elif self.mode == 'startWithExternalAgent':
            self.assert_otel_installed(health_instances[0])

        elif self.mode == 'installByAttach':
            self.assert_otel_installed(health_instances[0])

        elif self.mode == 'installByBackend':
            self.install_external_agent_param["instanceIds"] = health_instances[0]['instanceId']
            self.session.post(self.hot_plugging_url, json=self.install_external_agent_param, headers=self.header)

            health_instances = self.get_health_instances()
            self.assert_otel_installed(health_instances[0])

if __name__ == "__main__":
    unittest.main()
