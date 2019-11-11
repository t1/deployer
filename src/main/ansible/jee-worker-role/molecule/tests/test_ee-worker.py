import \
    os

import \
    testinfra.utils.ansible_runner

# TODO complete README.md

testinfra_hosts = testinfra.utils.ansible_runner.\
    AnsibleRunner(os.environ['MOLECULE_INVENTORY_FILE']).\
    get_hosts('all')


def test_deployer_is_installed(host):
    assert host.file('/opt/wildfly/standalone/deploy/deployer.war').is_file


# TODO reachable from the outside
# def test_deployer_is_reachable(host):
#     assert http localhost:8080/deployer contains something
