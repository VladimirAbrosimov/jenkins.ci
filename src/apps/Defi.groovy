package apps

import ru.abrosimov.jenkins.ci.context.Application

class Defi implements Application {
    String image = "vabrosimov/defi"
    String git = "git@github.com:vabrosimov/defi.git"
    String gitBranch = "master"
    String version = ""

    @Override
    void setVersion(String version) {
        this.version = version
    }
}

return new Defi()