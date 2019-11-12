import os
import testinfra.utils.ansible_runner

# TODO complete README.md
# TODO var for the list of deployables in the root bundle

testinfra_hosts = testinfra.utils.ansible_runner.\
    AnsibleRunner(os.environ['MOLECULE_INVENTORY_FILE']).\
    get_hosts('all')


def test_deployer_is_installed(host):
    deployer_war = '/opt/wildfly/standalone/deployments/deployer.war'
    assert host.file(deployer_war).is_file


# TODO test reachable
# def test_deployer_is_reachable(host):
#     assert http localhost:8080/deployer contains something
