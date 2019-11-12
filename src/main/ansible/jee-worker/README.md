jee-worker-role
===============

A [Jakarta-EE](https://jakarta.ee) container (namely [Wildfly](https://wildfly.org)) running the [Deployer](https://github.com/t1/deployer), ready to act as a worker in a [kub-ee](https://github.com/t1/kub-ee) cluster.

Requirements
------------

I had difficulties with Molecule and the ANSIBLE_ROLES_PATH. The path didn't include `~/.ansible/roles`. So I did `ln -s ~/.ansible/roles/ molecule/roles`, but I had to git-ignore it, as it's relative to _my_ home.

Role Variables
--------------

A description of the settable variables for this role should go here, including
any variables that are in defaults/main.yml, vars/main.yml, and any variables
that can/should be set via parameters to the role. Any variables that are read
from other roles and/or the global scope (ie. hostvars, group vars, etc.) should
be mentioned here as well.

Dependencies
------------

A list of other roles hosted on Galaxy should go here, plus any details in
regards to parameters that may need to be set for other roles, or variables that
are used from other roles.

Example Playbook
----------------

Including an example of how to use your role (for instance, with variables
passed in as parameters) is always nice for users too:

    - hosts: servers
      roles:
         - { role: jee-worker-role, x: 42 }

License
-------

Apache 2.0

Author Information
------------------

An optional section for the role authors to include contact information, or a
website (HTML is not allowed).
