package apps

import ru.abrosimov.jenkins.ci.context.Application

class Defi implements Application {
    String image = "vabrosimov/defi"
    String git = "git@github.com:vabrosimov/defi.git"
}

return new Defi()