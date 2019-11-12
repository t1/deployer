jee-worker-role
===============

A [Jakarta-EE](https://jakarta.ee) container (namely [Wildfly](https://wildfly.org)) running the [Deployer](https://github.com/t1/deployer), ready to act as a worker in a [kub-ee](https://github.com/t1/kub-ee) cluster (although it doesn't know about kub-ee).

Note that it installs a `deployer.root.bundle` with currently only one deployable: `jolokia`.

Requirements
------------

I had difficulties with the ANSIBLE_ROLES_PATH. It didn't include `~/.ansible/roles`. Molecule seems to override the default value `~/.ansible/roles:/usr/share/ansible/roles:/etc/ansible/roles`. Setting it in the `molecule.yml/provisioner` didn't help. So I did `ln -s ~/.ansible/roles/ molecule/roles`, but I had to git-ignore it, as the link is relative to _my_ home.

Role Variables
--------------

| var                       | description                            | default        |
| ------------------------- | -------------------------------------- | -------------- |
| deployer_version          | Version of the Deployer maven artifact | 3.0.0-SNAPSHOT |
| deployer_default_group_id | Deployer `default.group-id` config     | com.github.t1  |

Dependencies
------------

* [t1.jee_app_server](https://galaxy.ansible.com/t1/jee_app_server)

Example Playbook
----------------

    - hosts: workers
      roles:
         - { role: t1.jee_worker }

License
-------

Apache 2.0
