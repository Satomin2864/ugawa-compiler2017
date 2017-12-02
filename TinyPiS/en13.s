	.equ nchar, 10
	.section .data
_Pi_var_answer:
	.word 0
	.section .text
	.global _start
_start:
	@ 式をコンパイルした命令列
	ldr r0, =#90
	str r1, [sp, #-4]!
	str r2, [sp, #-4]!
	str r3, [sp, #-4]!
	str r4, [sp, #-4]!
	str r5, [sp, #-4]!
	str r6, [sp, #-4]!
	str r7, [sp, #-4]!
	ldr r1, =buf + nchar
	mov r2, #10
	mov r6, #1
L0:
	udiv r4, r0, r2
	mul r5, r4, r2
	sub r7, r0, r5
	sub r1, r1, #1
	add r7, r7, #48
	strb r7, [r1]
	add r6, r6, #1
	mov r0, r4
	cmp r0, #0
	bhi L0
	mov r7, #4
	mov r0, #1
	mov r2, r6
	swi #0
	ldr r7, [sp], #4
	ldr r6, [sp], #4
	ldr r5, [sp], #4
	ldr r4, [sp], #4
	ldr r3, [sp], #4
	ldr r2, [sp], #4
	ldr r1, [sp], #4
	@ EXITシステムコール
	ldr r0, =_Pi_var_answer
	ldr r0, [r0, #0]
	mov r7, #1
	swi #0
	.section .data
buf: .space nchar,0x30
	.byte 0x0a
