workflow "New workflow" {
  on = "push"
  resolves = ["build"]
}

action "build" {
  uses = "LucaFeger/action-maven-cli@aed8a1fd96b459b9a0be4b42a5863843cc70724e"
  args = "clean install"
}
