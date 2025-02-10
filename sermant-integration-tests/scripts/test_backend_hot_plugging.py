#
# Copyright (C) 2024-2024 Sermant Authors. All rights reserved.
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

class TestConfig(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.mode = os.getenv('TEST_MODE','default')

    hot_plugging_url = "http://127.0.0.1:8910/sermant/publishHotPluggingConfig"
    query_plugin_info_url = "http://127.0.0.1:8910/sermant/getPluginsInfo"
    query_install_result_url = "http://127.0.0.1:8915/testInstallPlugin"
    query_uninstall_result_url = "http://127.0.0.1:8915/testUninstallPlugin"
    query_update_result_url = "http://127.0.0.1:8915/testUpdatePlugin"
    header = {'content-type': 'application/json'}
    install_plugin_param = {
        "commandType": "INSTALL-PLUGINS",
        "instanceIds": "",
        "pluginNames": "dynamic-test-first-plugin,dynamic-test-second-plugin"
    }
    unInstall_plugin_param = {
        "commandType": "UNINSTALL-PLUGINS",
        "instanceIds": "",
        "pluginNames": "dynamic-test-first-plugin,dynamic-test-second-plugin"
    }
    update_plugin_param = {
        "commandType": "UPDATE-PLUGINS",
        "instanceIds": "",
        "pluginNames": "dynamic-test-first-plugin,dynamic-test-second-plugin"
    }

    def test_hot_plugging_function(self):
        session = requests.session()
        resp = session.get(self.query_plugin_info_url).json()
        self.assertTrue(resp[0].get('dynamicInstall'))
        self.unInstall_plugin_param["instanceIds"] = resp[0].get('instanceId')
        self.install_plugin_param["instanceIds"] = resp[0].get('instanceId')
        self.update_plugin_param["instanceIds"] = resp[0].get('instanceId')
        session.close()
        if self.mode == 'install':
            session = requests.session()
            session.post(self.hot_plugging_url, json=self.install_plugin_param, headers=self.header).json()
            time.sleep(20)
            resp = session.get(self.query_install_result_url).json()
            self.assertTrue(resp.get('DYNAMIC_INSTALL_PLUGIN_REPEAT_ENHANCE'))
        elif self.mode == 'update':
            session = requests.session()
            session.post(self.hot_plugging_url, json=self.update_plugin_param, headers=self.header).json()
            time.sleep(20)
            resp = session.get(self.query_update_result_url).json()
            self.assertTrue(resp.get('DYNAMIC_UPDATE_PLUGIN'))
        elif self.mode == 'unInstall':
            session = requests.session()
            session.post(self.hot_plugging_url, json=self.unInstall_plugin_param, headers=self.header).json()
            time.sleep(20)
            resp = session.get(self.query_uninstall_result_url).json()
            self.assertTrue(resp.get('DYNAMIC_UNINSTALL_PLUGIN_INTERCEPTOR_FAILURE'))
            self.assertFalse(resp.get('DYNAMIC_UNINSTALL_REPEAT_ENHANCE'))
if __name__ == "__main__":
    unittest.main()
