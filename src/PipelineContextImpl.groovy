import ru.abrosimov.jenkins.ci.context.Application
import ru.abrosimov.jenkins.ci.context.PipelineContext

class PipelineContextImpl extends PipelineContext {

    PipelineContextImpl(Object jenkins) {
        super(jenkins)
    }

    String registry = "95.174.94.249:8082/repository/registry/"
    Application application = null
}

return new PipelineContextImpl(this)