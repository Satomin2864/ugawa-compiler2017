import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import parser.PiLangLexer;
import parser.PiLangParser;

public class Compiler extends CompilerBase {
	boolean check_print = false;
	Environment globalEnv;
	
	void compileFunction(ASTFunctionNode nd) {
		Environment env = new Environment();
		String epilogueLabel = freshLabel();
		for (int i = 0; i < nd.params.size(); i++) {
			String name = nd.params.get(i);
			int offset = 4 * (i + 1);
			LocalVariable var = new LocalVariable(name, offset);
			env.push(var);
		}

		/* ここにローカル変数の追加コードをかく */
//		演習19
		for (int i = 0; i < nd.varDecls.size(); i++) {
			String name = nd.varDecls.get(i);
			int offset = -4 * (i + 3);
			LocalVariable var = new LocalVariable(name, offset);
			env.push(var);
		}
		
		emitLabel(nd.name);
		System.out.println("\t@ prologue");
		emitPUSH(REG_FP);
		emitRR("mov", REG_FP, REG_SP);
		emitPUSH(REG_LR);
		emitPUSH(REG_R1);
		emitRRI("sub", REG_SP, REG_SP, nd.varDecls.size() * 4);
		for (ASTNode stmt: nd.stmts)
			compileStmt(stmt, epilogueLabel, env);
		emitRI("mov", REG_DST, 0);
		emitLabel(epilogueLabel);
		/*ここにエピローグを生成するコードを書く*/
//		演習19
		System.out.println("\t@epilogue");
		emitRRI("add", REG_SP, REG_SP, nd.varDecls.size()*4);
		emitPOP(REG_R1);
		emitPOP(REG_LR);
		emitPOP(REG_FP);
		emitRET();
	}
	
	void compileStmt(ASTNode ndx, String epilogueLabel, Environment env) {
		/* ここを完成させる */
		if (ndx instanceof ASTCompoundStmtNode) {
//			この中を考える
			ASTCompoundStmtNode nd = (ASTCompoundStmtNode) ndx;
			ArrayList<ASTNode> stmts = nd.stmts;
			for (ASTNode child: stmts)
				compileStmt(child, epilogueLabel, env);
		} else if (ndx instanceof ASTAssignStmtNode) {
			ASTAssignStmtNode nd = (ASTAssignStmtNode) ndx;
			Variable var = env.lookup(nd.var);
			if (var == null)
				var = globalEnv.lookup(nd.var);
			if (var == null)
				throw new Error("undefined variable: "+nd.var);
			compileExpr(nd.expr, env);
			if (var instanceof GlobalVariable) {
				GlobalVariable globalVar = (GlobalVariable) var;
				emitLDC(REG_R1, globalVar.getLabel());
				emitSTR(REG_DST, REG_R1, 0);
			}
//			演習19
			else if (var instanceof LocalVariable){
				LocalVariable localVar = (LocalVariable) var;
				emitSTR(REG_DST, REG_FP, localVar.offset);
			}
			else
				throw new Error("Not a global variable: "+nd.var);
		} else if (ndx instanceof ASTIfStmtNode) {
			ASTIfStmtNode nd = (ASTIfStmtNode) ndx;
			String elseLabel = freshLabel();
			String endLabel = freshLabel();
			compileExpr(nd.cond, env);
			emitRI("cmp", REG_DST, 0);
			emitJMP("beq", elseLabel);
			compileStmt(nd.thenClause, epilogueLabel, env);
			emitJMP("b", endLabel);
			emitLabel(elseLabel);
			compileStmt(nd.elseClause, epilogueLabel, env);
			emitLabel(endLabel);
		} else if (ndx instanceof ASTWhileStmtNode) {
//			考える
			ASTWhileStmtNode nd = (ASTWhileStmtNode) ndx;
			String whileLabel = freshLabel();
			String whileEndLabel = freshLabel();
			emitLabel(whileLabel);
			compileExpr(nd.cond, env);
			emitRI("cmp", REG_DST, 0);
			emitJMP("beq", whileEndLabel);
			compileStmt(nd.stmt, epilogueLabel, env);
			emitJMP("b", whileLabel);
			emitLabel(whileEndLabel);
		}
//		演習19
		else if(ndx instanceof ASTReturnNode) {
			ASTReturnNode nd = (ASTReturnNode) ndx;
			compileExpr(nd.expr, env);
			emitJMP("b", epilogueLabel);
		}
//		Print文の作成
		 else if (ndx instanceof ASTPrintStmtNode) {
			ASTPrintStmtNode nd = (ASTPrintStmtNode) ndx;
			String loopLabel = freshLabel();
//			String Hexadecimal = freshLabel();
//			String ENDHexadecimal = freshLabel();
			compileExpr(nd.expr, env);
			emitPUSH(REG_R1);
			emitPUSH(REG_R2);
			emitPUSH(REG_R3);
			emitPUSH(REG_R4);
			emitPUSH(REG_R5);
			emitPUSH(REG_R6);
			emitPUSH(REG_R7);

			
			System.out.println("\tldr r1, =buf + nchar");
			emitRI("mov", REG_R2, 10);
			emitRI("mov", REG_R6, 1);
			
			emitLabel(loopLabel);
			emitRRR("udiv", REG_R4, REG_DST, REG_R2);
			emitRRR("mul", REG_R5, REG_R4, REG_R2);
			emitRRR("sub", REG_R7, REG_DST, REG_R5);
			emitRRI("sub", REG_R1, REG_R1, 1);
			emitRRI("add", REG_R7, REG_R7, '0');
			System.out.println("\tstrb r7, [r1]");	
			emitRRI("add", REG_R6, REG_R6, 1);
			emitRR("mov", REG_DST, REG_R4);
			emitRI("cmp", REG_DST, 0);
			emitJMP("bhi", loopLabel);
			
			emitRI("mov", REG_R7, 4);
			emitRI("mov", REG_DST, 1);
			emitRR("mov", REG_R2, REG_R6);
			emitI("swi", 0);


			emitPOP(REG_R7);
			emitPOP(REG_R6);
			emitPOP(REG_R5);
			emitPOP(REG_R4);
			emitPOP(REG_R3);
			emitPOP(REG_R2);
			emitPOP(REG_R1);
		}
		else
			throw new Error("Unkown expression: "+ndx);
	}

	void compileExpr(ASTNode ndx, Environment env) {
		/* ここを完成させる */
		if (ndx instanceof ASTBinaryExprNode) {
			ASTBinaryExprNode nd = (ASTBinaryExprNode) ndx;
			compileExpr(nd.lhs, env);
			emitPUSH(REG_R1);
			emitRR("mov", REG_R1, REG_DST);
			compileExpr(nd.rhs, env);
			if (nd.op.equals("+"))
				emitRRR("add", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("-"))
				emitRRR("sub", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("*"))
				emitRRR("mul", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("/"))
				emitRRR("udiv", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("|"))
				emitRRR("orr", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("&"))
				emitRRR("and", REG_DST, REG_R1, REG_DST);
			else
				throw new Error("Unknwon operator: "+nd.op);
			emitPOP(REG_R1);
		}
//		マイナスとnotの処理
		else if (ndx instanceof ASTUnaryExprNode){
			ASTUnaryExprNode nd = (ASTUnaryExprNode) ndx;
			compileExpr(nd.operand, env);
			if (nd.op.equals("-")){
				emitRR("mvn", REG_DST, REG_DST);
				emitRRI("add", REG_DST, REG_DST, 1);
			} else if (nd.op.equals("~"))
				emitRR("mvn", REG_DST, REG_DST);
			else
				throw new Error("Unknwon operator: "+nd.op);
		} else if (ndx instanceof ASTNumberNode) {
			ASTNumberNode nd = (ASTNumberNode) ndx;
			emitLDC(REG_DST, nd.value);
		}
//		演習19
		else if(ndx instanceof ASTVarRefNode){
			ASTVarRefNode nd = (ASTVarRefNode) ndx;
			Variable var = env.lookup(nd.varName);
			if (var == null)
				var = globalEnv.lookup(nd.varName);
			if (var == null)
				throw new Error("Unknown variable: "+nd.varName);
			if (var instanceof GlobalVariable) {
				GlobalVariable globalVar = (GlobalVariable) var;
				emitLDC(REG_DST, globalVar.getLabel());
				emitLDR(REG_DST, REG_DST, 0);
			} else {
				LocalVariable localVar = (LocalVariable) var;
				int offset = localVar.offset;
				emitLDR(REG_DST, REG_FP, offset);
			} 
		}
//			演習19
		else if(ndx instanceof ASTCallNode) {
			ASTCallNode nd = (ASTCallNode) ndx;
			ArrayList<ASTNode> args = nd.args;
			Collections.reverse(args);
			for (ASTNode arg: args){
				compileExpr(arg, env);
				emitPUSH(REG_DST);
			}
			emitJMP("bl", nd.name);
			emitRRI("add", REG_SP, REG_SP, nd.args.size() * 4);
			
		}else 
			throw new Error("Unknown expression: "+ndx);
	}
	
	void compile(ASTNode ast) {
		globalEnv = new Environment();
		ASTProgNode program = (ASTProgNode) ast;

		System.out.println("\t.section .data");
		System.out.println("\t@ 大域変数の定義");		
		for (String varName: program.varDecls) {
			if (globalEnv.lookup(varName) != null)
				throw new Error("Variable redefined: "+varName);
			GlobalVariable v = addGlobalVariable(globalEnv, varName);
			emitLabel(v.getLabel());
			System.out.println("\t.word 0");
		}

		System.out.println("\t.section .text");
		System.out.println("\t.global _start");
		System.out.println("_start:");
		System.out.println("\t@ main関数を呼出す．戻り値は r0 に入る");
		emitJMP("bl", "main");
		System.out.println("\t@ EXITシステムコール");
		emitRI("mov", "r7", 1);      // EXIT のシステムコール番号
		emitI("swi", 0);
		
		/* 関数定義 */
		for (ASTFunctionNode func: program.funcDecls)
			compileFunction(func);
	}

	public static void main(String[] args) throws IOException {
		ANTLRInputStream input = new ANTLRInputStream(System.in);
		PiLangLexer lexer = new PiLangLexer(input);
		CommonTokenStream token = new CommonTokenStream(lexer);
		PiLangParser parser = new PiLangParser(token);
		ParseTree tree = parser.prog();
		ASTGenerator astgen = new ASTGenerator();
		ASTNode ast = astgen.translate(tree);
		Compiler compiler = new Compiler();
		compiler.compile(ast);
	}
}
