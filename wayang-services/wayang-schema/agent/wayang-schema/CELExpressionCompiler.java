
// CEL Compiler
@ApplicationScoped
public class CELExpressionCompiler {
    public CompiledExpression compile(String expression) {
        try {
            // Use CEL-Java library
            Env env = Env.newEnv();
            Ast ast = env.compile(expression).getAst();
            Program program = env.program(ast);
            
            return new CompiledExpression(expression, program);
        } catch (Exception e) {
            throw new CELCompilationException("Failed to compile: " + expression, e);
        }
    }
    
    public Object evaluate(CompiledExpression compiled, Map<String, Object> variables) {
        try {
            return compiled.getProgram().eval(variables);
        } catch (Exception e) {
            throw new CELEvaluationException("Failed to evaluate: " + compiled.getExpression(), e);
        }
    }
}