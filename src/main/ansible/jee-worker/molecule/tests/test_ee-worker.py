import os
import testinfra.utils.ansible_runner


testinfra_hosts = testinfra.utils.ansible_runner.\
    AnsibleRunner(os.environ['MOLECULE_INVENTORY_FILE']).\
    get_hosts('all')


# This test is quite unnecessary, as the role does the most important checks
def test_deployer_is_installed(host):
    marker = '/opt/wildfly/standalone/deployments/deployer.war.deployed'
    assert host.file(marker).is_file
