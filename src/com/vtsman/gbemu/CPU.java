package com.vtsman.gbemu;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's Z80 CPU by associating a java function with each Z80
//opcode
public class CPU {
	//These constants represent various ALU flags
	//This flag is set when the result of an ALU operation is zero
	private static final int FLAG_ZERO = 1 << 7;
	//This flag is set when the ALU subtracts 2 registers or decrements a register
	private static final int FLAG_NEG = 1 << 6;
	//This flag is set when an operation requires a carry between 2 hex digits
	private static final int FLAG_HALF_CARRY = 1 << 5;
	//This flag is set when a register overflows
	private static final int FLAG_CARRY = 1 << 4;

	//The next two arrays store information about the opcodes
	//Each instruction takes 4 arguments: a disassembly string,
	//The number of arguments (in bytes), the associated java function,
	//And the number of clock cycles each instruction takes

	//The 'this::method' syntax is known as a method handle, and is a java 8
	//feature which allows one to automatically make a class which implements
	//an interface that contains a single function whose signature matches that
	//of the specified method
	//@formatter:off
	public Instruction[] instructions = {
			new Instruction("NOP", 0, this::nop, 4),
			new Instruction("LD BC 0x%hex%", 2, this::ldiBC, 12),
			new Instruction("LD (BC) A", 0, this::writeBCptrAbyte, 8),
			new Instruction("INC BC", 0, this::incBC, 8),
			new Instruction("INC B", 0, this::incB, 4),
			new Instruction("DEC B", 0, this::decB, 4),
			new Instruction("LD B 0x%hex%", 1, this::ldiB, 8),
			new Instruction("RLC A", 0, this::rlca, 4),
			new Instruction("LD (0x%hex%) SP", 2, this::ldPtrSP, 20),
			new Instruction("ADD HL BC", 0, this::addHlBc, 8),
			new Instruction("LD A (BC)", 0, this::ldABCptr, 8),
			new Instruction("DEC BC", 0, this::decBC, 8),
			new Instruction("INC C", 0, this::incC, 4),
			new Instruction("DEC C", 0, this::decC, 4),
			new Instruction("LD C 0x%hex%", 1, this::ldiC, 8),
			new Instruction("RRC A", 0, this::rrca, 4),

			new Instruction("STOP", 0, this::nop, 4), //TODO implement?
			new Instruction("LD DE 0x%hex%", 2, this::ldDE, 12),
			new Instruction("LD (DE) A", 0, this::ldDEptrA, 8),
			new Instruction("INC DE", 0, this::incDE, 8),
			new Instruction("INC D", 0, this::incD, 4),
			new Instruction("DEC D", 0, this::decD, 4),
			new Instruction("LD D 0x%hex%", 1, this::ldiD, 8),
			new Instruction("RL A", 0, this::rl2A, 4),
			new Instruction("JR %signed%", 1, this::relJump, 12),
			new Instruction("ADD HL DE", 0, this::addHlDe, 8),
			new Instruction("LD A (DE)", 0, this::ldADEptr, 8),
			new Instruction("DEC DE", 0, this::decDE, 8),
			new Instruction("INC E", 0, this::incE, 4),
			new Instruction("DEC E", 0, this::decE, 4),
			new Instruction("LD E 0x%hex%", 1, this::lde, 8),
			new Instruction("RR A", 0, this::rr2a, 4),

			new Instruction("JR NZ %signed%", 1, this::relJumpNotZero, 0),
			new Instruction("LD HL 0x%hex%", 2, this::ldhl, 12),
			new Instruction("LDI (HL) A", 0, this::writeHLIptrAbyte, 8),
			new Instruction("INC HL", 0, this::incHL, 8),
			new Instruction("INC H", 0, this::incH, 4),
			new Instruction("DEC H", 0, this::decH, 4),
			new Instruction("LD H 0x%hex%", 1, this::ldHN, 8),
			new Instruction("DAA", 0, this::daa, 4),
			new Instruction("JR Z %signed%", 1, this::relJumpZero, 0),
			new Instruction("ADD HL HL", 0, this::addHlHl, 8),
			new Instruction("LDI A (HL)", 0, this::ldIncHLPtr, 8),
			new Instruction("DEC HL", 0, this::decHL, 8),
			new Instruction("INC L", 0, this::incL, 4),
			new Instruction("DEC L", 0, this::decL, 4),
			new Instruction("LD L 0x%hex%", 1, this::ldiL, 8),
			new Instruction("CPL", 0, this::cpl, 4),

			new Instruction("JR NC %signed%", 1, this::relJumpNoCarry, 0),
			new Instruction("LD SP 0x%hex%", 2, this::ldsp, 12),
			new Instruction("LDD (HL) A", 0, this::lddHlA, 8),
			new Instruction("INC SP", 0, this::incSP, 8),
			new Instruction("INC (HL)", 0, this::incHLptr, 12),
			new Instruction("DEC (HL)", 0, this::decHLptr, 12),
			new Instruction("LD (HL) 0x%hex%", 1, this::writeHLptrNbyte, 12),
			new Instruction("SCF", 0, this::setCarry, 4),
			new Instruction("JR C %signed%", 1, this::relJumpCarry, 0),
			new Instruction("ADD HL SP", 0, this::addHlSp, 8),
			new Instruction("LDD A (HL)", 0, this::lddAHl, 8),
			new Instruction("DEC SP", 0, this::decSP, 8),
			new Instruction("INC A", 0, this::incA, 4),
			new Instruction("DEC A", 0, this::decA, 4),
			new Instruction("LD A 0x%hex%", 1, this::lda, 8),
			new Instruction("CCF", 0, this::ccf, 4),

			new Instruction("LD B B", 0, this::nop, 4),
			new Instruction("LD B C", 0, this::ldbc, 4),
			new Instruction("LD B D", 0, this::ldbd, 4),
			new Instruction("LD B E", 0, this::ldbe, 4),
			new Instruction("LD B H", 0, this::ldbh, 4),
			new Instruction("LD B L", 0, this::ldbl, 4),
			new Instruction("LD B (HL)", 0, this::ldBHLptr, 8),
			new Instruction("LD B A", 0, this::ldba, 4),
			new Instruction("LD C B", 0, this::ldcb, 4),
			new Instruction("LD C C", 0, this::nop, 4),
			new Instruction("LD C D", 0, this::ldcd, 4),
			new Instruction("LD C E", 0, this::ldce, 4),
			new Instruction("LD C H", 0, this::ldch, 4),
			new Instruction("LD C L", 0, this::ldcl, 4),
			new Instruction("LD C (HL)", 0, this::ldCHLptr, 8),
			new Instruction("LD C A", 0, this::ldCA, 4),

			new Instruction("LD D B", 0, this::ldDB, 4),
			new Instruction("LD D C", 0, this::ldDC, 4),
			new Instruction("LD D D", 0, this::nop, 4),
			new Instruction("LD D E", 0, this::ldDe, 4),
			new Instruction("LD D H", 0, this::ldDH, 4),
			new Instruction("LD D L", 0, this::ldDL, 4),
			new Instruction("LD D (HL)", 0, this::ldDHlPtr, 8),
			new Instruction("LD D A", 0, this::ldDA, 4),
			new Instruction("LD E B", 0, this::ldEB, 4),
			new Instruction("LD E C", 0, this::ldEC, 4),
			new Instruction("LD E D", 0, this::ldED, 4),
			new Instruction("LD E E", 0, this::nop, 4),
			new Instruction("LD E H", 0, this::ldEH, 4), //The most canadian instruction
			new Instruction("LD E L", 0, this::ldEL, 4),
			new Instruction("LD E (HL)", 0, this::ldEHlPtr, 8),
			new Instruction("LD E A", 0, this::ldEA, 4),

			new Instruction("LD H B", 0, this::ldHB, 4),
			new Instruction("LD H C", 0, this::ldHC, 4),
			new Instruction("LD H D", 0, this::ldHD, 4),
			new Instruction("LD H E", 0, this::ldHE, 4),
			new Instruction("LD H H", 0, this::nop, 4),
			new Instruction("LD H L", 0, this::ldHL, 4),
			new Instruction("LD H (HL)", 0, this::ldHHLPtr, 8),
			new Instruction("LD H A", 0, this::ldHA, 4),
			new Instruction("LD L B", 0, this::ldLB, 4),
			new Instruction("LD L C", 0, this::ldLC, 4),
			new Instruction("LD L D", 0, this::ldLD, 4),
			new Instruction("LD L E", 0, this::ldLE, 4),
			new Instruction("LD L H", 0, this::ldLH, 4),
			new Instruction("LD L L", 0, this::nop, 4),
			new Instruction("LD L (HL)", 0, this::ldLHlPtr, 8),
			new Instruction("LD L A", 0, this::ldLA, 4),

			new Instruction("LD (HL) B", 0, this::ldHLPtrB, 8),
			new Instruction("LD (HL) C", 0, this::ldHLPtrC, 8),
			new Instruction("LD (HL) D", 0, this::ldHLPtrD, 8),
			new Instruction("LD (HL) E", 0, this::ldHLPtrE, 8),
			new Instruction("LD (HL) H", 0, this::ldHLPtrH, 8),
			new Instruction("LD (HL) L", 0, this::ldHLPtrL, 8),
			new Instruction("HALT", 0, this::halt, 4),
			new Instruction("LD (HL) A", 0, this::ldHLptrA, 8),
			new Instruction("LD B A", 0, this::ldab, 4),
			new Instruction("LD C A", 0, this::ldac, 4),
			new Instruction("LD A D", 0, this::ldAD, 4),
			new Instruction("LD A E", 0, this::ldAE, 4),
			new Instruction("LD A H", 0, this::ldAH, 4),
			new Instruction("LD A L", 0, this::ldAL, 4),
			new Instruction("LD A (HL)", 0, this::ldAHlPtr, 8),
			new Instruction("LD A A", 0, this::nop, 4),

			new Instruction("ADD A B", 0, this::addAB, 4),
			new Instruction("ADD A C", 0, this::addAC, 4),
			new Instruction("ADD A D", 0, this::addAD, 4),
			new Instruction("ADD A E", 0, this::addAE, 4),
			new Instruction("ADD A H", 0, this::addAH, 4),
			new Instruction("ADD A L", 0, this::addAL, 4),
			new Instruction("ADD A (HL)", 0, this::addAPtr, 8),
			new Instruction("ADD A A", 0, this::addAA, 4),
			new Instruction("ADC A B", 0, this::adcAB, 4),
			new Instruction("ADC A C", 0, this::adcAC, 4),
			new Instruction("ADC A D", 0, this::adcAD, 4),
			new Instruction("ADC A E", 0, this::adcAE, 4),
			new Instruction("ADC A H", 0, this::adcAH, 4),
			new Instruction("ADC A L", 0, this::adcAL, 4),
			new Instruction("ADC A (HL)", 0, this::adcAPtr, 8),
			new Instruction("ADC A A", 0, this::adcAA, 4),

			new Instruction("SUB A B", 0, this::subAB, 4),
			new Instruction("SUB A C", 0, this::subAC, 4),
			new Instruction("SUB A D", 0, this::subAD, 4),
			new Instruction("SUB A E", 0, this::subAE, 4),
			new Instruction("SUB A H", 0, this::subAH, 4),
			new Instruction("SUB A L", 0, this::subAL, 4),
			new Instruction("SUB A (HL)", 0, this::subAPtr, 8),
			new Instruction("SUB A A", 0, this::subAA, 4),
			new Instruction("SBC A B", 0, this::sbcAB, 4),
			new Instruction("SBC A C", 0, this::sbcAC, 4),
			new Instruction("SBC A D", 0, this::sbcAD, 4),
			new Instruction("SBC A E", 0, this::sbcAE, 4),
			new Instruction("SBC A H", 0, this::sbcAH, 4),
			new Instruction("SBC A L", 0, this::sbcAL, 4),
			new Instruction("SBC A (HL)", 0, this::sbcAPtr, 8),
			new Instruction("SBC A A", 0, this::sbcAA, 4),

			new Instruction("AND B", 0, this::andBA, 4),
			new Instruction("AND C", 0, this::andCA, 4),
			new Instruction("AND D", 0, this::andDA, 4),
			new Instruction("AND E", 0, this::andEA, 4),
			new Instruction("AND H", 0, this::andHA, 4),
			new Instruction("AND L", 0, this::andLA, 4),
			new Instruction("AND (HL)", 0, this::andPtr, 8),
			new Instruction("AND A", 0, this::andAA, 4),
			new Instruction("XOR B", 0, this::xorBA, 4),
			new Instruction("XOR C", 0, this::xorCA, 4),
			new Instruction("XOR D", 0, this::xorDA, 4),
			new Instruction("XOR E", 0, this::xorEA, 4),
			new Instruction("XOR H", 0, this::xorHA, 4),
			new Instruction("XOR L", 0, this::xorLA, 4),
			new Instruction("XOR (HL)", 0, this::xorPtrA, 8),
			new Instruction("XOR A", 0, this::xorAA, 4),

			new Instruction("OR B", 0, this::orBA, 4),
			new Instruction("OR C", 0, this::orCA, 4),
			new Instruction("OR D", 0, this::orDA, 4),
			new Instruction("OR E", 0, this::orEA, 4),
			new Instruction("OR H", 0, this::orHA, 4),
			new Instruction("OR L", 0, this::orLA, 4),
			new Instruction("OR (HL)", 0, this::orPtr, 8),
			new Instruction("OR A", 0, this::orAA, 4),
			new Instruction("CP B", 0, this::cpb, 4),
			new Instruction("CP C", 0, this::cpc, 4),
			new Instruction("CP D", 0, this::cpd, 4),
			new Instruction("CP E", 0, this::cpe, 4),
			new Instruction("CP H", 0, this::cph, 4),
			new Instruction("CP L", 0, this::cpL, 4),
			new Instruction("CP (HL)", 0, this::cpPtr, 8),
			new Instruction("CP A", 0, this::cpa, 4),

			new Instruction("RET NZ", 0, this::retIfNotZero, 0),
			new Instruction("POP BC", 0, this::popBC, 12),
			new Instruction("JP NZ 0x%hex%", 2, this::jumpNotZero, 0),
			new Instruction("JP 0x%hex%", 2, this::jmpi, 16),
			new Instruction("CALL NZ 0x%hex%", 2, this::callIfNotZero, 0),
			new Instruction("PUSH BC", 0, this::pushBC, 16),
			new Instruction("ADD A 0x%hex%", 1, this::addAN, 8),
			new Instruction("RST 0", 0, this::rst0, 16),
			new Instruction("RET Z", 0, this::retIfZero, 0),
			new Instruction("RET", 0, this::ret, 16),
			new Instruction("JP Z 0x%hex%", 2, this::jumpZero, 0),
			new ExtendedInstructions(),
			new Instruction("CALL Z 0x%hex%", 2, this::callIfZero, 0),
			new Instruction("CALL 0x%hex%", 2, this::call, 24),
			new Instruction("ADC A 0x%hex%", 1, this::adcA, 8),
			new Instruction("RST 8", 0, this::rst8, 16),

			new Instruction("RET NC", 0, this::retIfNotCarry, 0),
			new Instruction("POP DE", 0, this::popDE, 12),
			new Instruction("JP NC 0x%hex%", 2, this::jumpNotCarry, 0),
			new Nonexistant(0xd3),
			new Instruction("CALL NC 0x%hex%", 2, this::callIfNotCarry, 0),
			new Instruction("PUSH DE", 0, this::pushDE, 16),
			new Instruction("SUB A 0x%hex%", 1, this::subA, 8),
			new Instruction("RST 10", 0, this::rst10, 16),
			new Instruction("RET C", 0, this::retIfCarry, 0),
			new Instruction("RETI", 0, this::reti, 16),
			new Instruction("JP C 0x%hex%", 2, this::jumpCarry, 0),
			new Nonexistant(0xdb),
			new Instruction("CALL C 0x%hex%", 2, this::callIfCarry, 0),
			new Nonexistant(0xdd),
			new Instruction("SBC A 0x%hex%", 1, this::sbcA, 8),
			new Instruction("RST 18", 0, this::rst18, 16),

			new Instruction("LDH 0xff%hex% A", 1, this::ldhNA, 12),
			new Instruction("POP HL", 0, this::popHL, 12),
			new Instruction("LDH (C) A", 0, this::ldhCA, 8),
			new Nonexistant(0xe3),
			new Nonexistant(0xe4),
			new Instruction("PUSH HL", 0, this::pushHL, 16),
			new Instruction("AND 0x%hex%", 1, this::andi, 8),
			new Instruction("RST 20", 0, this::rst20, 16),
			new Instruction("ADD SP 0x%hex%", 1, this::addSpN, 16),
			new Instruction("JMP (HL)", 0, this::jmpHL, 4),
			new Instruction("LD (0x%hex%) A", 2, this::ldNA, 16),
			new Nonexistant(0xeb),
			new Nonexistant(0xec),
			new Nonexistant(0xed),
			new Instruction("XOR 0x%hex%", 1, this::xorNA, 8),
			new Instruction("RST 28", 0, this::rst28, 16),

			new Instruction("LDH A 0xff%hex%", 1, this::ldhAN, 12),
			new Instruction("POP AF", 0, this::popAF, 12),
			new Instruction("LD A (0xff00 + C)", 0, this::ldAUpper, 8),
			new Instruction("DI", 0, this::di, 4),
			new Nonexistant(0xf4),
			new Instruction("PUSH AF", 0, this::pushAF, 16),
			new Instruction("OR 0x%hex%", 1, this::orN, 8),
			new Instruction("RST 30", 0, this::rst30, 16),
			new Instruction("LD HL SP 0x%hex%", 1, this::ldHlSpN, 12),
			new Instruction("LD SP HL", 0, this::ldSpHl, 8),
			new Instruction("LD A (0x%hex%)", 2, this::ldPtrA, 16),
			new Instruction("EI", 0, this::ei, 4),
			new Nonexistant(0xfc),
			new Nonexistant(0xfd),
			new Instruction("CP 0x%hex%", 1, this::cpn, 8),
			new Instruction("RST 38", 0, this::rst38, 16),
	};
	//@formatter:on

	//@formatter:off
	public Instruction[] extInstructions = {
		new Instruction("RLC B", 0, this::rlcB, 8),
		new Instruction("RLC C", 0, this::rlcC, 8),
		new Instruction("RLC D", 0, this::rlcD, 8),
		new Instruction("RLC E", 0, this::rlcE, 8),
		new Instruction("RLC H", 0, this::rlcH, 8),
		new Instruction("RLC L", 0, this::rlcL, 8),
		new Instruction("RLC (HL)", 0, this::rlcPtr, 16),
		new Instruction("RLC A", 0, this::rlcA, 8),
		new Instruction("RRC B", 0, this::rrcB, 8),
		new Instruction("RRC C", 0, this::rrcC, 8),
		new Instruction("RRC D", 0, this::rrcD, 8),
		new Instruction("RRC E", 0, this::rrcE, 8),
		new Instruction("RRC H", 0, this::rrcH, 8),
		new Instruction("RRC L", 0, this::rrcL, 8),
		new Instruction("RRC (HL)", 0, this::rrcPtr, 16),
		new Instruction("RRC A", 0, this::rrcA, 8),

		new Instruction("RL B", 0, this::rlB, 8),
		new Instruction("RL C", 0, this::rlC, 8),
		new Instruction("RL D", 0, this::rlD, 8),
		new Instruction("RL E", 0, this::rlE, 8),
		new Instruction("RL H", 0, this::rlH, 8),
		new Instruction("RL L", 0, this::rlL, 8),
		new Instruction("RL (HL)", 0, this::rlPtr, 16),
		new Instruction("RL A", 0, this::rlA, 8),
		new Instruction("RR B", 0, this::rrb, 8),
		new Instruction("RR C", 0, this::rrc, 8),
		new Instruction("RR D", 0, this::rrd, 8),
		new Instruction("RR E", 0, this::rre, 8),
		new Instruction("RR H", 0, this::rrh, 8),
		new Instruction("RR L", 0, this::rrl, 8),
		new Instruction("RR (HL)", 0, this::rrptr, 16),
		new Instruction("RR A", 0, this::rra, 8),

		new Instruction("SLA B", 0, this::slaB, 8),
		new Instruction("SLA C", 0, this::slaC, 8),
		new Instruction("SLA D", 0, this::slaD, 8),
		new Instruction("SLA E", 0, this::slaE, 8),
		new Instruction("SLA H", 0, this::slaH, 8),
		new Instruction("SLA L", 0, this::slaL, 8),
		new Instruction("SLA (HL)", 0, this::slaPtr, 16),
		new Instruction("SLA A", 0, this::slaA, 8),
		new Instruction("SRA B", 0, this::sraB, 8),
		new Instruction("SRA C", 0, this::sraC, 8),
		new Instruction("SRA D", 0, this::sraD, 8),
		new Instruction("SRA E", 0, this::sraE, 8),
		new Instruction("SRA H", 0, this::sraH, 8),
		new Instruction("SRA L", 0, this::sraL, 8),
		new Instruction("SRA Ptr", 0, this::sraPtr, 16),
		new Instruction("SRA A", 0, this::sraA, 8),

		new Instruction("SWAP B", 0, this::swapB, 8),
		new Instruction("SWAP C", 0, this::swapC, 8),
		new Instruction("SWAP D", 0, this::swapD, 8),
		new Instruction("SWAP E", 0, this::swapE, 8),
		new Instruction("SWAP H", 0, this::swapH, 8),
		new Instruction("SWAP L", 0, this::swapL, 8),
		new Instruction("SWAP (HL)", 0, this::swapPtr, 16),
		new Instruction("SWAP A", 0, this::swapA, 8),
		new Instruction("SRL B", 0, this::srlB, 8),
		new Instruction("SRL C", 0, this::srlC, 8),
		new Instruction("SRL D", 0, this::srlD, 8),
		new Instruction("SRL E", 0, this::srlE, 8),
		new Instruction("SRL H", 0, this::srlH, 8),
		new Instruction("SRL L", 0, this::srlL, 8),
		new Instruction("SRL (HL)", 0, this::srlPtr, 16),
		new Instruction("SRL A", 0, this::srlA, 8),

		new Instruction("BIT 0 B", 0, this::bit0B, 8),
		new Instruction("BIT 0 C", 0, this::bit0C, 8),
		new Instruction("BIT 0 D", 0, this::bit0D, 8),
		new Instruction("BIT 0 E", 0, this::bit0E, 8),
		new Instruction("BIT 0 H", 0, this::bit0H, 8),
		new Instruction("BIT 0 L", 0, this::bit0L, 8),
		new Instruction("BIT 0 (HL)", 0, this::bit0HL, 12),
		new Instruction("BIT 0 A", 0, this::bit0A, 8),
		new Instruction("BIT 1 B", 0, this::bit1B, 8),
		new Instruction("BIT 1 C", 0, this::bit1C, 8),
		new Instruction("BIT 1 D", 0, this::bit1D, 8),
		new Instruction("BIT 1 E", 0, this::bit1E, 8),
		new Instruction("BIT 1 H", 0, this::bit1H, 8),
		new Instruction("BIT 1 L", 0, this::bit1L, 8),
		new Instruction("BIT 1 (HL)", 0, this::bit1HL, 12),
		new Instruction("BIT 1 A", 0, this::bit1A, 8),

		new Instruction("BIT 2 B", 0, this::bit2B, 8),
		new Instruction("BIT 2 C", 0, this::bit2C, 8),
		new Instruction("BIT 2 D", 0, this::bit2D, 8),
		new Instruction("BIT 2 E", 0, this::bit2E, 8),
		new Instruction("BIT 2 H", 0, this::bit2H, 8),
		new Instruction("BIT 2 L", 0, this::bit2L, 8),
		new Instruction("BIT 2 (HL)", 0, this::bit2HL, 12),
		new Instruction("BIT 2 A", 0, this::bit2A, 8),
		new Instruction("BIT 3 B", 0, this::bit3B, 8),
		new Instruction("BIT 3 C", 0, this::bit3C, 8),
		new Instruction("BIT 3 D", 0, this::bit3D, 8),
		new Instruction("BIT 3 E", 0, this::bit3E, 8),
		new Instruction("BIT 3 H", 0, this::bit3H, 8),
		new Instruction("BIT 3 L", 0, this::bit3L, 8),
		new Instruction("BIT 3 (HL)", 0, this::bit3HL, 12),
		new Instruction("BIT 3 A", 0, this::bit3A, 8),

		new Instruction("BIT 4 B", 0, this::bit4B, 8),
		new Instruction("BIT 4 C", 0, this::bit4C, 8),
		new Instruction("BIT 4 D", 0, this::bit4D, 8),
		new Instruction("BIT 4 E", 0, this::bit4E, 8),
		new Instruction("BIT 4 H", 0, this::bit4H, 8),
		new Instruction("BIT 4 L", 0, this::bit4L, 8),
		new Instruction("BIT 4 (HL)", 0, this::bit4HL, 12),
		new Instruction("BIT 4 A", 0, this::bit4A, 8),
		new Instruction("BIT 5 B", 0, this::bit5B, 8),
		new Instruction("BIT 5 C", 0, this::bit5C, 8),
		new Instruction("BIT 5 D", 0, this::bit5D, 8),
		new Instruction("BIT 5 E", 0, this::bit5E, 8),
		new Instruction("BIT 5 H", 0, this::bit5H, 8),
		new Instruction("BIT 5 L", 0, this::bit5L, 8),
		new Instruction("BIT 5 (HL)", 0, this::bit5HL, 12),
		new Instruction("BIT 5 A", 0, this::bit5A, 8),

		new Instruction("BIT 6 B", 0, this::bit6B, 8),
		new Instruction("BIT 6 C", 0, this::bit6C, 8),
		new Instruction("BIT 6 D", 0, this::bit6D, 8),
		new Instruction("BIT 6 E", 0, this::bit6E, 8),
		new Instruction("BIT 6 H", 0, this::bit6H, 8),
		new Instruction("BIT 6 L", 0, this::bit6L, 8),
		new Instruction("BIT 6 (HL)", 0, this::bit6HL, 12),
		new Instruction("BIT 6 A", 0, this::bit6A, 8),
		new Instruction("BIT 7 B", 0, this::bit7B, 8),
		new Instruction("BIT 7 C", 0, this::bit7C, 8),
		new Instruction("BIT 7 D", 0, this::bit7D, 8),
		new Instruction("BIT 7 E", 0, this::bit7E, 8),
		new Instruction("BIT 7 H", 0, this::bit7H, 8),
		new Instruction("BIT 7 L", 0, this::bit7L, 8),
		new Instruction("BIT 7 (HL)", 0, this::bit7HL, 12),
		new Instruction("BIT 7 A", 0, this::bit7A, 8),

		new Instruction("RES 0 B", 0, this::res0b, 8),
		new Instruction("RES 0 C", 0, this::res0c, 8),
		new Instruction("RES 0 D", 0, this::res0d, 8),
		new Instruction("RES 0 E", 0, this::res0e, 8),
		new Instruction("RES 0 H", 0, this::res0h, 8),
		new Instruction("RES 0 L", 0, this::res0l, 8),
		new Instruction("RES 0 (HL)", 0, this::res0Ptr, 16),
		new Instruction("RES 0 A", 0, this::res0a, 8),
		new Instruction("RES 1 B", 0, this::res1b, 8),
		new Instruction("RES 1 C", 0, this::res1c, 8),
		new Instruction("RES 1 D", 0, this::res1d, 8),
		new Instruction("RES 1 E", 0, this::res1e, 8),
		new Instruction("RES 1 H", 0, this::res1h, 8),
		new Instruction("RES 1 L", 0, this::res1l, 8),
		new Instruction("RES 1 (HL)", 0, this::res1Ptr, 16),
		new Instruction("RES 1 A", 0, this::res1a, 8),

		new Instruction("RES 2 B", 0, this::res2b, 8),
		new Instruction("RES 2 C", 0, this::res2c, 8),
		new Instruction("RES 2 D", 0, this::res2d, 8),
		new Instruction("RES 2 E", 0, this::res2e, 8),
		new Instruction("RES 2 H", 0, this::res2h, 8),
		new Instruction("RES 2 L", 0, this::res2l, 8),
		new Instruction("RES 2 (HL)", 0, this::res2Ptr, 16),
		new Instruction("RES 2 A", 0, this::res2a, 8),
		new Instruction("RES 3 B", 0, this::res3b, 8),
		new Instruction("RES 3 C", 0, this::res3c, 8),
		new Instruction("RES 3 D", 0, this::res3d, 8),
		new Instruction("RES 3 E", 0, this::res3e, 8),
		new Instruction("RES 3 H", 0, this::res3h, 8),
		new Instruction("RES 3 L", 0, this::res3l, 8),
		new Instruction("RES 3 (HL)", 0, this::res3Ptr, 16),
		new Instruction("RES 3 A", 0, this::res3a, 8),

		new Instruction("RES 4 B", 0, this::res4b, 8),
		new Instruction("RES 4 C", 0, this::res4c, 8),
		new Instruction("RES 4 D", 0, this::res4d, 8),
		new Instruction("RES 4 E", 0, this::res4e, 8),
		new Instruction("RES 4 H", 0, this::res4h, 8),
		new Instruction("RES 4 L", 0, this::res4l, 8),
		new Instruction("RES 4 (HL)", 0, this::res4Ptr, 16),
		new Instruction("RES 4 A", 0, this::res4a, 8),
		new Instruction("RES 5 B", 0, this::res5b, 8),
		new Instruction("RES 5 C", 0, this::res5c, 8),
		new Instruction("RES 5 D", 0, this::res5d, 8),
		new Instruction("RES 5 E", 0, this::res5e, 8),
		new Instruction("RES 5 H", 0, this::res5h, 8),
		new Instruction("RES 5 L", 0, this::res5l, 8),
		new Instruction("RES 5 (HL)", 0, this::res5Ptr, 16),
		new Instruction("RES 5 A", 0, this::res5a, 8),

		new Instruction("RES 6 B", 0, this::res6b, 8),
		new Instruction("RES 6 C", 0, this::res6c, 8),
		new Instruction("RES 6 D", 0, this::res6d, 8),
		new Instruction("RES 6 E", 0, this::res6e, 8),
		new Instruction("RES 6 H", 0, this::res6h, 8),
		new Instruction("RES 6 L", 0, this::res6l, 8),
		new Instruction("RES 6 (HL)", 0, this::res6Ptr, 16),
		new Instruction("RES 6 A", 0, this::res6a, 8),
		new Instruction("RES 7 B", 0, this::res7b, 8),
		new Instruction("RES 7 C", 0, this::res7c, 8),
		new Instruction("RES 7 D", 0, this::res7d, 8),
		new Instruction("RES 7 E", 0, this::res7e, 8),
		new Instruction("RES 7 H", 0, this::res7h, 8),
		new Instruction("RES 7 L", 0, this::res7l, 8),
		new Instruction("RES 7 (HL)", 0, this::res7Ptr, 16),
		new Instruction("RES 7 A", 0, this::res7a, 8),

		new Instruction("SET 0 B", 0, this::set0B, 8),
		new Instruction("SET 0 C", 0, this::set0C, 8),
		new Instruction("SET 0 D", 0, this::set0D, 8),
		new Instruction("SET 0 E", 0, this::set0E, 8),
		new Instruction("SET 0 H", 0, this::set0H, 8),
		new Instruction("SET 0 L", 0, this::set0L, 8),
		new Instruction("SET 0 (HL)", 0, this::set0Ptr, 16),
		new Instruction("SET 0 A", 0, this::set0A, 8),
		new Instruction("SET 1 B", 0, this::set1B, 8),
		new Instruction("SET 1 C", 0, this::set1C, 8),
		new Instruction("SET 1 D", 0, this::set1D, 8),
		new Instruction("SET 1 E", 0, this::set1E, 8),
		new Instruction("SET 1 H", 0, this::set1H, 8),
		new Instruction("SET 1 L", 0, this::set1L, 8),
		new Instruction("SET 1 (HL)", 0, this::set1Ptr, 16),
		new Instruction("SET 1 A", 0, this::set1A, 8),

		new Instruction("SET 2 B", 0, this::set2B, 8),
		new Instruction("SET 2 C", 0, this::set2C, 8),
		new Instruction("SET 2 D", 0, this::set2D, 8),
		new Instruction("SET 2 E", 0, this::set2E, 8),
		new Instruction("SET 2 H", 0, this::set2H, 8),
		new Instruction("SET 2 L", 0, this::set2L, 8),
		new Instruction("SET 2 (HL)", 0, this::set2Ptr, 16),
		new Instruction("SET 2 A", 0, this::set2A, 8),
		new Instruction("SET 3 B", 0, this::set3B, 8),
		new Instruction("SET 3 C", 0, this::set3C, 8),
		new Instruction("SET 3 D", 0, this::set3D, 8),
		new Instruction("SET 3 E", 0, this::set3E, 8),
		new Instruction("SET 3 H", 0, this::set3H, 8),
		new Instruction("SET 3 L", 0, this::set3L, 8),
		new Instruction("SET 3 (HL)", 0, this::set3Ptr, 16),
		new Instruction("SET 3 A", 0, this::set3A, 8),

		new Instruction("SET 4 B", 0, this::set4B, 8),
		new Instruction("SET 4 C", 0, this::set4C, 8),
		new Instruction("SET 4 D", 0, this::set4D, 8),
		new Instruction("SET 4 E", 0, this::set4E, 8),
		new Instruction("SET 4 H", 0, this::set4H, 8),
		new Instruction("SET 4 L", 0, this::set4L, 8),
		new Instruction("SET 4 (HL)", 0, this::set4Ptr, 16),
		new Instruction("SET 4 A", 0, this::set4A, 8),
		new Instruction("SET 5 B", 0, this::set5B, 8),
		new Instruction("SET 5 C", 0, this::set5C, 8),
		new Instruction("SET 5 D", 0, this::set5D, 8),
		new Instruction("SET 5 E", 0, this::set5E, 8),
		new Instruction("SET 5 H", 0, this::set5H, 8),
		new Instruction("SET 5 L", 0, this::set5L, 8),
		new Instruction("SET 5 (HL)", 0, this::set5Ptr, 16),
		new Instruction("SET 5 A", 0, this::set5A, 8),

		new Instruction("SET 6 B", 0, this::set6B, 8),
		new Instruction("SET 6 C", 0, this::set6C, 8),
		new Instruction("SET 6 D", 0, this::set6D, 8),
		new Instruction("SET 6 E", 0, this::set6E, 8),
		new Instruction("SET 6 H", 0, this::set6H, 8),
		new Instruction("SET 6 L", 0, this::set6L, 8),
		new Instruction("SET 6 (HL)", 0, this::set6Ptr, 16),
		new Instruction("SET 6 A", 0, this::set6A, 8),
		new Instruction("SET 7 B", 0, this::set7B, 8),
		new Instruction("SET 7 C", 0, this::set7C, 8),
		new Instruction("SET 7 D", 0, this::set7D, 8),
		new Instruction("SET 7 E", 0, this::set7E, 8),
		new Instruction("SET 7 H", 0, this::set7H, 8),
		new Instruction("SET 7 L", 0, this::set7L, 8),
		new Instruction("SET 7 (HL)", 0, this::set7Ptr, 16),
		new Instruction("SET 7 A", 0, this::set7A, 8),
	};
	//@formatter:on

	// double registers. Use bit shifting to address single registers

	private int af;
	private int bc;
	private int de;
	private int hl;

	// Program counter and stack pointer
	private int pc;
	private int sp;

	// This should always be the MMU, but I'm letting it be an IAddressable
	// because I can and it'll be helpful in testing
	private IAddressable mem;
	//The clock is used primarily to update the GPU and interrupts
	private Clock c;

	//This is the master interrupt flag, which is set
	//by the instructions DI and EI
	private boolean intsEnabled = false;
	//This flag is set when the interrupt flag is queued to change next instruction
	private boolean queuedIntChange = false;
	//This flag stores the queued state of the interrupt flag
	private boolean queuedIntState = false;

	private boolean doHaltBug = false;
	//This scanner is used in the breakpoint function
	private Scanner sc = new Scanner(System.in);

	//This keeps track of all set breakpoints
	private ArrayList<Integer> breakPoints = new ArrayList<Integer>();

	//The halt flag is used in the halt operation, and allows the CPU to
	//wait for an interrupt
	//private int haltFlag = 0;
	private volatile boolean haltFlag = false;

	//This variable was used to disable logging when in an interrupt method.
	//I think it's useless now
	private boolean inInterrupt = false;

	private boolean inBP = false;
	private int lastPC = 0;

	public CPU(IAddressable m, Clock clock, boolean logInterrupts) {
		this.mem = m;
		this.c = clock;
		this.reset();
	}

	//This function was used to encourage myself. Every time I ran the emulator,
	//it would tell me how many instructions I had implemented
	public int implemented() {
		int implemented = 0;
		for (Instruction i : this.instructions) {
			if (!(i instanceof Unimplemented)) {
				implemented++;
			}
		}
		return implemented;
	}

	public boolean halted() {
		return this.haltFlag;
	}

	//This *theoretically* allows one to reset the gameboy's state.
	private void reset() {
		// Reset registers
		// Set program counter to 0x100, right past the BIOS
		this.pc = 0x100;
		this.sp = 0xfffe;

		//Set state to match what the BIOS would've set it to.
		if ((this.mem.read(0x143) & 0x80) == 0x80) {
			this.af = 0x1180;
			this.bc = 0x0000;
			this.de = 0xff56;
			this.hl = 0x000d;
			System.out.println("Initialized as GBC");
		} else {
			this.af = 0x01b0;
			this.bc = 0x0013;
			this.de = 0x00d8;
			this.hl = 0x014d;
			System.out.println("Initialized as GB");
		}

		//Reset the clock counter
		this.c.reset();

		//Reset IO memory
		/*for (int i = 0; i < 0x80; i++) {
			this.mem.write(0xff00 | i, (byte) IO.ioReset[i]);
		}
		System.out.println("============");//TODO*/
	}

	//This function was used in debugging to allow me to add breakpoints
	public void addBreakPoint(int i) {
		this.breakPoints.add(i);
	}

	//This function was never used, but nice to have. Does what it says.
	public void removeBreakPoint(int i) {
		this.breakPoints.remove(this.breakPoints.indexOf(i));
	}


	private boolean didHaltBug = false;
	//This function checks for a breakpoint, and runs the appropriate opcode
	public void execute() {
		if (this.breakPoints.indexOf(this.pc) > -1 || (this.inBP && this.lastPC != this.pc)) {
			//TODO remove?
			bp();
		}

		if(!this.haltFlag){
			//Gets the opcode
			int inst = this.mem.read(this.pc) & 0xff;
			//Halts if the instruction is unimplemented
			if (!instructions[inst].implemented(this.pc)) {
				System.out.printf("0x%04x: %s\n", this.pc, instructions[inst].dissassemble(this.pc));
				for (;;)
					;
			}
			//Increments the program counter, then runs the opcode
			int prev = this.pc;
			if(!this.doHaltBug) {
				this.pc++;
			}
			else{
				this.doHaltBug = false;
				didHaltBug = true;
				this.pc--;
				System.out.println(Integer.toHexString(this.pc));
			}
			execOpcode(inst, prev);
			didHaltBug = false;
		}
		else{
			this.c.inc(1);
		}
	}

	//This function executes a specific opcode
	public void execOpcode(int inst, int base) {
		//System.out.printf("CP>0x%04x\n", base);
		//Store if an interrupt switch is queued
		boolean flip = this.queuedIntChange;
		//Gets the arguments that follow the instruction
		int[] args = this.readBytesFromMemory(instructions[inst].getArgCount(base));
		if(didHaltBug){
			System.out.println(inst);
			System.out.println(args[0]);
		}
		//Execute the opcode
		instructions[inst].getMethod(base).execute(args);
		//Update the timer
		this.c.inc(instructions[inst].baseTicks);
		if(flip){
			this.intsEnabled = this.queuedIntState;
			this.queuedIntChange = false;
		}
	}

	//This function was used for debugging. It ran an extended instruction
	public void execExtOpcode(int inst, int base) {
		int[] args = this.readBytesFromMemory(extInstructions[inst].getArgCount(base));
		extInstructions[inst].getMethod(base).execute(args);
		this.c.inc(extInstructions[inst].baseTicks);
	}

	//This is the breakpoint function. It allowed me to step through code
	//And inspect registers
	public void bp() {
		while(true){
			//Prompt for user input
			System.out.print("> ");
			String s = sc.nextLine();
			//'s' means step
			this.inBP = true;
			this.lastPC = this.pc;
			if (s.equals("s")) {
				System.out.printf("0x%04X: %s\n", this.pc,
						instructions[this.mem.read(this.pc) & 0xff].dissassemble(this.pc));
				break;
			} else if (s.equals("d")) { //'d' means dump
				System.out.printf("AF: 0x%04x\n", af);
				System.out.printf("BC: 0x%04x\n", bc);
				System.out.printf("DE: 0x%04x\n", de);
				System.out.printf("HL: 0x%04x\n", hl);
				System.out.printf("PC: 0x%04x\n", pc);
				System.out.printf("SP: 0x%04x\n", sp);
				System.out.println("Interrupts: " + this.intsEnabled);
			} else if (s.startsWith("dump ")) {//'dump' also means dump, but it dumps a memory value
				String num = s.substring("dump ".length());
				if (num.startsWith("0x")) {
					num = num.substring(2);
				}
				int i = Integer.parseInt(num, 16);
				System.out.printf("Byte at 0x%04x is 0x%02x\n", i, this.mem.read(i));
			} else if (s.equals("b")) {//'b' breaks out of the breakpoint
				this.inBP = false;
				break;
			} else if (s.startsWith("bp")) {//'b' breaks out of the breakpoint
				this.addBreakPoint(Integer.parseInt(s.substring(3), 16));
			}else if (s.startsWith("rp")) {//'b' breaks out of the breakpoint
				this.removeBreakPoint(Integer.parseInt(s.substring(3), 16));
			}
		}
	}

	public void reqBP(){
		this.inBP = true;
	}

	//This function was meant for, you guessed it, debugging! It ran an opcode and
	//printed out the dissassembly in real time.
	public void debugExecute() {
		//f ((this.haltFlag & 2) == 0 && (!this.inInterrupt))
			System.out.printf("0x%04x: %s\n", this.pc,
					instructions[this.mem.read(this.pc) & 0xff].dissassemble(this.pc));
		this.execute();
	}

	//No op, does nothing
	private void nop(int[] asdf) { //0x00

	}

	//Reads a value from C, stores it in B
	private void ldiBC(int[] args) { //0x01
		this.bc = args[0] << 8 | args[1];
	}

	//Writes the byte in register A into the address stored in BC
	private void writeBCptrAbyte(int[] asdf) { //0x02
		this.mem.write(this.bc, (byte) (this.af >> 8)); //TODO need to reverse? This is little endian.
	}

	//Increments BC
	private void incBC(int[] asdf) { //0x03
		this.bc = (this.bc + 1) & 0xffff;
	}

	//Increments B
	private void incB(int[] asdf) { //0x04
		this.bc = (this.bc & 0xff) | ((incByte(this.bc >> 8) & 0xff) << 8);
	}

	//Decrements B
	private void decB(int[] asdf) { //0x05
		this.bc = (this.bc & 0xff) | ((dec(this.bc >> 8) & 0xff) << 8);
	}

	//Loads the next byte into B
	private void ldiB(int[] b) { //0x06
		this.bc = (b[0] << 8) | (this.bc & 0xff);
	}

	//Rotate left carry register A
	//A bit shift left by one, but takes bit 7 and set bit 0 to it
	private void rlca(int[] asdf) { //0x07
		int carry = ((this.af >> 8) & 0x80) >> 7;
		//Set carry flag if bit is carried
		if (carry > 0) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}

		//Rotate
		int a = (this.af >> 8) & 0xff;
		a <<= 1;
		a |= carry;

		//Wipe all extra bits
		a &= 0xff;

		//Set the register
		this.af = (this.af & 0xff) | (a << 8);

		//Reset other flags
		this.af &= ~(FLAG_ZERO | FLAG_NEG | FLAG_HALF_CARRY);
	}

	//Set stack pointer to next constant in memory
	private void ldPtrSP(int[] ptr) { //0x08
		this.mem.writeShort((ptr[0] << 8) | ptr[1], (short) this.sp);
	}

	//Add double registers HL and BC, store in HL
	private void addHlBc(int[] asdf) { //0x09
		this.hl = addShorts((short) this.hl, (short) this.bc) & 0xffff;
	}

	//Load the value in memory at address BC into register A
	private void ldABCptr(int[] asdf) { //0x0a
		this.af = (this.af & 0xff) | ((this.mem.read(this.bc) & 0xff) << 8);
	}

	//Decrement BC
	private void decBC(int[] asdf) { //0x0b
		this.bc = (this.bc - 1) & 0xffff;
	}

	//Increment C
	private void incC(int[] asdf) { //0x0c
		this.bc = (this.bc & 0xff00) | (incByte(this.bc & 0xff) & 0xff);
	}

	//Decrement C
	private void decC(int[] asdf) { //0x0d
		this.bc = (this.bc & 0xff00) | (dec(this.bc & 0xff) & 0xff);
	}

	//Load next byte into register C
	private void ldiC(int[] c) { //0x0e
		this.bc = (c[0]) | (this.bc & 0xff00);
	}

	//Rotate right carry register A
	private void rrca(int[] asdf) { //0x0f
		this.af = ((this.rrc2((byte) (this.af >> 8)) & 0xff) << 8) | (this.af & 0xff);
	}

	//Load next constant into DE
	private void stop(int[] val) { //0x10
		int pval = this.mem.read(0xff4d);
		if((pval & 0x1) == 0x1){
			if((pval & 0x80) == 0x80){
				this.mem.write(0xff4d, (byte)0);
			}
			else {
				this.mem.write(0xff4d, (byte)0x80);
			}
		}
	}

	//Load next constant into DE
	private void ldDE(int[] val) { //0x11
		this.de = ((val[0] << 8) | val[1]);
	}

	//Write byte in register A into the memory at the address stored in DE
	private void ldDEptrA(int[] asdf) { //0x12
		this.mem.write(this.de, (byte) (this.af >> 8));
	}

	//Increment DE
	private void incDE(int[] asdf) { //0x13
		this.de = (this.de + 1) & 0xffff;
	}

	//Increment D
	private void incD(int[] asdf) { //0x14
		this.de = (this.de & 0xff) | ((incByte((this.de >> 8) & 0xff) & 0xff) << 8);
	}

	//Decrement D
	private void decD(int[] asdf) { //0x15
		this.de = (this.de & 0xff) | ((dec((this.de >> 8) & 0xff) & 0xff) << 8);
	}

	//Set D to next constant
	private void ldiD(int[] d) { //0x16
		this.de = (d[0] << 8) | (this.de & 0xff);
	}

	//An alternate rotate left A operation
	private void rl2A(int[] asdf) { //0x17
		int carry = (this.af & FLAG_CARRY) != 0 ? 1 : 0;

		this.af &= 0xff00;

		if ((this.af & (1 << 7) << 8) != 0) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}

		this.af = (this.af & 0xff) | ((this.af << 1) & 0xff00) | (carry << 8);
	}

	//Relative jump by next byte
	private void relJump(int[] off) { //0x18
		this.pc += (byte) off[0]; //Cast to byte to allow for negative jumps
	}

	//Add registers HL and DE, store in HL
	private void addHlDe(int[] asdf) { //0x19
		this.hl = addShorts((short) this.hl, (short) this.de) & 0xffff;
	}

	//Load the value in memory at the pointer stored in DE into register A
	private void ldADEptr(int[] asdf) { //0x1a
		this.af = (this.af & 0xff) | ((this.mem.read(this.de) & 0xff) << 8);
	}

	//Decrement DE
	private void decDE(int[] asdf) { //0x1b
		this.de = (this.de - 1) & 0xffff;
	}

	//Increment E
	private void incE(int[] asdf) { //0x1c
		this.de = (this.de & 0xff00) | (incByte(this.de & 0xff) & 0xff);
	}

	//Decrement E
	private void decE(int[] asdf) { //0x1d
		this.de = (this.de & 0xff00) | (dec(this.de & 0xff) & 0xff);
	}

	//Load next constant into register E
	private void lde(int[] val) { //0x1e
		this.de = (this.de & 0xff00) | (val[0]);
	}

	//Alternate rotate right A
	private void rr2a(int[] asdf) { //0x1f
		int carry = (((this.af & FLAG_CARRY) != 0) ? 1 : 0) << 7;

		if ((this.af & (0x01 << 8)) != 0) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}
		int a = (this.af >> 8) & 0xff;
		a >>= 1;
		a |= carry;

		a &= 0xff;

		this.af &= ~(FLAG_NEG | FLAG_ZERO | FLAG_HALF_CARRY);

		this.af = (a << 8) | (this.af & 0xff);
	}

	//Relative jump if the zero flag is not set
	private void relJumpNotZero(int[] off) { //0x20
		if ((this.af & FLAG_ZERO) == 0) {
			this.relJump(off);
			this.c.inc(12);
		} else {
			this.c.inc(8);
		}
	}

	//Load next constant into HL
	private void ldhl(int[] val) { //0x21
		this.hl = ((val[0] << 8) | val[1]);
	}

	//Write value in A to memory at pointer stored in HL, increment HL
	private void writeHLIptrAbyte(int[] asdf) { //0x22
		this.mem.write(this.hl, (byte) (this.af >> 8)); //TODO need to reverse? This is little endian.
		this.hl = (this.hl + 1) & 0xffff;
	}

	//Increment HL
	private void incHL(int[] asdf) { //0x23
		this.hl = (this.hl + 1) & 0xffff;
	}

	//Increment H
	private void incH(int[] asdf) { //0x24
		this.hl = ((incByte((this.hl >> 8) & 0xff) & 0xff) << 8) | (this.hl & 0xff);
	}

	//Decrement H
	private void decH(int[] asdf) { //0x25
		this.hl = ((dec((this.hl >> 8) & 0xff) & 0xff) << 8) | (this.hl & 0xff);
	}

	//Load next byte into H
	private void ldHN(int[] val) { //0x26
		this.hl = (this.hl & 0xff) | ((val[0] << 8) & 0xff00);
	}

	//No idea what this instruction's purpose is. Was a pain to code, though.
	private void daa(int[] asdf) { //0x27

		int a = (this.af >> 8) & 0xff;
		int f = this.af & 0xff;

		if ((f & FLAG_NEG) == 0) {
			if (((f & FLAG_HALF_CARRY) != 0) || ((a & 0xf) > 0x09)) {
				a += 0x06;
			}

			if (((f & FLAG_CARRY) != 0) || ((a) > 0x9f)) {
				a += 0x60;
			}
		} else {
			if (((f & FLAG_HALF_CARRY) != 0)) {
				a = ((a - 0x06) & 0xff);
			}

			if (((f & FLAG_CARRY) != 0)) {
				a -= 0x60;
			}
		}

		f &= ~(FLAG_HALF_CARRY | FLAG_ZERO);

		if ((a & 0x100) > 0) {
			f |= FLAG_CARRY;
		}

		a &= 0xff;

		if (a == 0) {
			f |= FLAG_ZERO;
		}

		af = ((a) << 8) | (f & 0xff);
	}

	//Perform a relative jump if the zero flag is set
	private void relJumpZero(int[] off) { //0x28
		if ((this.af & FLAG_ZERO) != 0) {
			this.relJump(off);
			this.c.inc(12);
		} else {
			this.c.inc(8);
		}
	}

	//Double HL
	private void addHlHl(int[] asdf) { //0x29
		this.hl = addShorts((short) this.hl, (short) this.hl) & 0xffff;
	}

	//Load the value stored at the pointer stored in HL into register A, increment HL
	private void ldIncHLPtr(int[] asdf) { //0x2a
		this.af = (this.af & 0xff) | ((this.mem.read(this.hl) & 0xff) << 8);
		this.hl = (this.hl + 1) & 0xffff;
	}

	//Decrement HL
	private void decHL(int[] asdf) { //0x2b
		this.hl = (this.hl - 1) & 0xffff;
	}

	//Increment L
	private void incL(int[] asdf) { //0x2c
		this.hl = (this.hl & 0xff00) | (incByte(this.hl & 0xff) & 0xff);
	}

	//Decrement L
	private void decL(int[] asdf) { //0x2d
		this.hl = (this.hl & 0xff00) | (dec(this.hl & 0xff) & 0xff);
	}

	//Load next byte into register L
	private void ldiL(int[] c) { //0x2e
		this.hl = (c[0]) | (this.hl & 0xff00);
	}

	//Invert register A
	private void cpl(int[] asdf) { //0x2f
		this.af = (this.af & 0xff) | ((~this.af) & 0xff00);
		this.af |= (FLAG_NEG | FLAG_HALF_CARRY);
	}

	//Relative jump if carry flag not set
	private void relJumpNoCarry(int[] off) { //0x30
		if ((this.af & FLAG_CARRY) == 0) {
			this.relJump(off);
			this.c.inc(12);
		} else {
			this.c.inc(8);
		}
	}

	//Load next value into stack pointer register
	private void ldsp(int[] val) { //0x31
		this.sp = ((val[0] << 8) | val[1]);
	}

	//Load value A into memory at pointer stored in HL, decrement HL
	private void lddHlA(int[] asdf) { //0x32
		this.mem.write(this.hl, (byte) (this.af >> 8));
		this.hl = (this.hl - 1) & 0xffff;
	}

	//Increment the stack pointer
	private void incSP(int[] asdf) { //0x33
		this.sp = (this.sp + 1) & 0xffff;
	}

	//Increment the value in memory stored at pointer in HL
	private void incHLptr(int[] asdf) { //0x34
		this.mem.write(this.hl, (byte) incByte(this.mem.read(this.hl)));
	}

	//Decrement the value in memory stored at pointer in HL
	private void decHLptr(int[] asdf) { //0x35
		this.mem.write(this.hl, (byte) dec(this.mem.read(this.hl)));
	}

	//Write the next byte into memory at pointer stored in HL
	private void writeHLptrNbyte(int[] asdf) { //0x36
		this.mem.write(this.hl, (byte) asdf[0]); //TODO need to reverse? This is little endian.
	}

	//Set the carry flag
	private void setCarry(int[] asdf) { //0x37
		this.af |= FLAG_CARRY;
		this.af &= ~(FLAG_NEG | FLAG_HALF_CARRY);
	}

	//Perform a relative jump if the carry flag isn't set
	private void relJumpCarry(int[] off) { //0x38
		if ((this.af & FLAG_CARRY) != 0) {
			this.relJump(off);
			this.c.inc(12);
		} else {
			this.c.inc(8);
		}
	}

	//Add HL and SP, store in HL
	private void addHlSp(int[] asdf) { //0x39
		this.hl = this.addShorts((short) this.hl, (short) this.sp);
	}

	//Load value in memory stored at pointer in HL into register A, decrement HL
	private void lddAHl(int[] asdf) { //0x3a
		this.af = ((this.mem.read(this.hl) & 0xff) << 8) | (this.af & 0xff);
		this.hl = (this.hl - 1) & 0xffff;
	}

	//Decrement stack pointer
	private void decSP(int[] asdf) { //0x3b
		this.sp = (this.sp - 1) & 0xffff;
	}

	//Increment A
	private void incA(int[] asdf) { //0x3c
		this.af = ((incByte((this.af >> 8) & 0xff) & 0xff) << 8) | (this.af & 0xff);
	}

	//Decrement A
	private void decA /*Is a cult*/(int[] asdf) { //0x3d
		this.af = ((dec((this.af >> 8) & 0xff) & 0xff) << 8) | (this.af & 0xff);
	}

	//Load next constant into register A
	private void lda(int[] val) { //0x3e
		this.af = (this.af & 0xff) | (val[0] << 8);
	}

	//Invert carry flag
	private void ccf(int[] asdf) { //0x3f
		if ((this.af & FLAG_CARRY) > 0) {
			this.af &= ~(FLAG_CARRY);
		} else {
			this.af |= FLAG_CARRY;
		}

		this.af &= ~(FLAG_NEG | FLAG_HALF_CARRY);
	}

	//load various registers or memory values into other registers
	private void ldbc(int[] asdf) { //0x41
		this.bc = (this.bc & 0xff) | ((this.bc & 0xff) << 8);
	}

	private void ldbd(int[] asdf) { //0x42
		this.bc = (this.bc & 0xff) | (this.de & 0xff00);
	}

	private void ldbe(int[] asdf) { //0x43
		this.bc = (this.bc & 0xff) | ((this.de << 8) & 0xff00);
	}

	private void ldbh(int[] asdf) { //0x44
		this.bc = (this.bc & 0xff) | (this.hl & 0xff00);
	}

	private void ldbl(int[] asdf) { //0x45
		this.bc = (this.bc & 0xff) | ((this.hl << 8) & 0xff00);
	}

	private void ldBHLptr(int[] asdf) { //0x46
		this.bc = (this.bc & 0xff) | ((this.mem.read(this.hl) & 0xff) << 8);
	}

	private void ldba(int[] asdf) { //0x47
		this.bc = (this.af & 0xff00) | (this.bc & 0x00ff);
	}

	private void ldcb(int[] asdf) { //0x48
		this.bc = (this.bc & 0xff00) | ((this.bc >> 8) & 0xff);
	}

	private void ldcd(int[] asdf) { //0x4a
		this.bc = (this.bc & 0xff00) | ((this.de >> 8) & 0x00ff);
	}

	private void ldce(int[] asdf) { //0x4b
		this.bc = (this.bc & 0xff00) | ((this.de) & 0x00ff);
	}

	private void ldch(int[] asdf) { //0x4c
		this.bc = (this.bc & 0xff00) | ((this.hl >> 8) & 0xff);
	}

	private void ldcl(int[] asdf) { //0x4d
		this.bc = (this.bc & 0xff00) | (this.hl & 0xff);
	}

	private void ldCHLptr(int[] asdf) { //0x4e
		this.bc = (this.bc & 0xff00) | (this.mem.read(this.hl) & 0xff);
	}

	private void ldCA(int[] asdf) { //0x4f
		this.bc = (this.bc & 0xff00) | ((this.af >> 8) & 0xff);
	}

	private void ldDB(int[] asdf) { //0x50
		this.de = (this.de & 0xff) | (this.bc & 0xff00);
	}

	private void ldDC(int[] asdf) { //0x51
		this.de = (this.de & 0xff) | ((this.bc << 8) & 0xff00);
	}

	private void ldDe(int[] asdf) { //0x53
		this.de = (this.de & 0xff) | ((this.de << 8) & 0xff00);
	}

	private void ldDH(int[] asdf) { //0x54
		this.de = (this.de & 0xff) | (this.hl & 0xff00);
	}

	private void ldDL(int[] asdf) { //0x55
		this.de = (this.de & 0xff) | ((this.hl << 8) & 0xff00);
	}

	private void ldDHlPtr(int[] asdf) { //0x56
		this.de = (this.de & 0xff) | ((this.mem.read(this.hl) & 0xff) << 8);
	}

	private void ldDA(int[] asdf) { //0x57
		this.de = (this.de & 0xff) | (this.af & 0xff00);
	}

	private void ldEB(int[] asdf) { //0x58
		this.de = (this.de & 0xff00) | ((this.bc >> 8) & 0xff);
	}

	private void ldEC(int[] asdf) { //0x59
		this.de = (this.de & 0xff00) | ((this.bc) & 0xff);
	}

	private void ldED(int[] asdf) { //0x5a
		this.de = (this.de & 0xff00) | ((this.de >> 8) & 0xff);
	}

	private void ldEH(int[] asdf) { //0x5b
		this.de = (this.de & 0xff00) | ((this.hl >> 8) & 0xff);
	}

	private void ldEL(int[] asdf) { //0x5d
		this.de = (this.de & 0xff00) | (this.hl & 0xff);
	}

	private void ldEHlPtr(int[] asdf) { //0x5e
		this.de = (this.de & 0xff00) | (this.mem.read(this.hl) & 0xff);
	}

	private void ldEA(int[] asdf) { //0x5f
		this.de = (this.de & 0xff00) | ((this.af >> 8) & 0xff);
	}

	private void ldHB(int[] asdf) { //0x60
		this.hl = (this.hl & 0xff) | (this.bc & 0xff00);
	}

	private void ldHC(int[] asdf) { //0x61
		this.hl = (this.hl & 0xff) | ((this.bc << 8) & 0xff00);
	}

	private void ldHD(int[] asdf) { //0x62
		this.hl = (this.hl & 0xff) | (this.de & 0xff00);
	}

	private void ldHE(int[] asdf) { //0x63
		this.hl = (this.hl & 0xff) | ((this.de << 8) & 0xff00);
	}

	private void ldHL(int[] asdf) { //0x65
		this.hl = (this.hl & 0xff) | ((this.hl << 8) & 0xff00);
	}

	private void ldHHLPtr(int[] asdf) { //0x66
		this.hl = (this.hl & 0xff) | ((this.mem.read(this.hl) & 0xff) << 8);
	}

	private void ldHA(int[] asdf) { //0x67
		this.hl = (this.hl & 0xff) | (this.af & 0xff00);
	}

	private void ldLB(int[] asdf) { //0x68
		this.hl = (this.hl & 0xff00) | ((this.bc >> 8) & 0xff);
	}

	private void ldLC(int[] asdf) { //0x69
		this.hl = (this.hl & 0xff00) | (this.bc & 0xff);
	}

	private void ldLD(int[] asdf) { //0x6a
		this.hl = (this.hl & 0xff00) | ((this.de >> 8) & 0xff);
	}

	private void ldLE(int[] asdf) { //0x6b
		this.hl = (this.hl & 0xff00) | (this.de & 0xff);
	}

	private void ldLH(int[] asdf) { //0x6c
		this.hl = (this.hl & 0xff00) | ((this.hl >> 8) & 0xff);
	}

	private void ldLHlPtr(int[] asdf) { //0x6e
		this.hl = (this.hl & 0xff00) | (this.mem.read(this.hl) & 0xff);
	}

	private void ldLA(int[] asdf) { //0x6f
		this.hl = (this.hl & 0xff00) | ((this.af >> 8) & 0xff);
	}

	private void ldHLPtrB(int[] asdf) { //0x70
		this.mem.write(this.hl, (byte) ((this.bc >> 8) & 0xff));
	}

	private void ldHLPtrC(int[] asdf) { //0x71
		this.mem.write(this.hl, (byte) (this.bc & 0xff));
	}

	private void ldHLPtrD(int[] asdf) { //0x72
		this.mem.write(this.hl, (byte) ((this.de >> 8) & 0xff));
	}

	private void ldHLPtrE(int[] asdf) { //0x73
		this.mem.write(this.hl, (byte) (this.de & 0xff));
	}

	private void ldHLPtrH(int[] asdf) { //0x74
		this.mem.write(this.hl, (byte) ((this.hl >> 8) & 0xff));
	}

	private void ldHLPtrL(int[] asdf) { //0x75
		this.mem.write(this.hl, (byte) (this.hl & 0xff));
	}

	private void halt(int[] asdf) { //0x76
		if (this.areIntsEnabled()) {
			/*if (this.haltFlag == 3) {
				this.haltFlag = 0;
			} else {
				this.haltFlag = 2;
				this.pc--;
			}*/
			//this.haltFlag = 2;
			this.haltFlag = true;
		} else {
			if(((this.mem.read(0xffff) & this.mem.read(0xff0f)) & 0x1f) == 0){
				/*if (this.haltFlag == 3) {
					this.haltFlag = 0;
				} else {
					this.haltFlag = 2;
					this.pc--;
				}*/
				this.haltFlag = true;
			}
			else{
				this.doHaltBug = true;
				System.out.println("Halt bug enabled");
			}
		}
	}

	private void ldHLptrA(int[] asdf) { //0x77
		this.mem.write(this.hl, (byte) (this.af >> 8));
	}

	private void ldab(int[] asdf) { //0x78
		this.af = (this.bc & 0xff00) | (this.af & 0x00ff);
	}

	private void ldac(int[] asdf) { //0x79
		this.af = ((this.bc & 0xff) << 8) | (this.af & 0x00ff);
	}

	private void ldAD(int[] asdf) { //0x7a
		this.af = (this.af & 0xff) | (this.de & 0xff00);
	}

	private void ldAE(int[] asdf) { //0x7b
		this.af = (this.af & 0xff) | ((this.de & 0xff) << 8);
	}

	private void ldAH(int[] asdf) { //0x7c
		this.af = (this.af & 0xff) | (this.hl & 0xff00);
	}

	private void ldAL(int[] asdf) { //0x7d
		this.af = (this.af & 0xff) | ((this.hl & 0xff) << 8);
	}

	private void ldAHlPtr(int[] asdf) { //0x7e
		this.af = (this.af & 0xff) | ((this.mem.read(this.hl) & 0xff) << 8);
	}

	//Add various registers
	private void addAB(int[] asdf) { //0x80
		this.af = ((this.addBytes((byte) (this.af >> 8), (byte) (this.bc >> 8)) & 0xff) << 8) | (this.af & 0xff);
	}

	private void addAC(int[] asdf) { //0x81
		this.af = (addBytes((byte) (this.af >> 8), (byte) (this.bc & 0xff)) << 8) | (this.af & 0xff);
	}

	private void addAD(int[] asdf) { //0x82
		this.af = (addBytes((byte) (this.af >> 8), (byte) ((this.de >> 8) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void addAE(int[] asdf) { //0x83
		this.af = (addBytes((byte) (this.af >> 8), (byte) (this.de & 0xff)) << 8) | (this.af & 0xff);
	}

	private void addAH(int[] asdf) { //0x84
		this.af = (addBytes((byte) (this.af >> 8), (byte) ((this.hl >> 8) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void addAL(int[] asdf) { //0x85
		this.af = (addBytes((byte) (this.af >> 8), (byte) (this.hl & 0xff)) << 8) | (this.af & 0xff);
	}

	private void addAPtr(int[] asdf) { //0x86
		this.af = (addBytes((byte) (this.af >> 8), (byte) (this.mem.read(this.hl) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void addAA(int[] asdf) { //0x87
		this.af = (addBytes((byte) (this.af >> 8), (byte) (this.af >> 8)) << 8) | (this.af & 0xff);
	}

	//Add with 2 numbers and carry flag (A + B + 1 if carry)
	private void adcAB(int[] asdf) { //0x88
		this.af = (((this.adcBytes((byte) ((this.af >> 8) & 0xff), (byte) ((this.bc >> 8) & 0xff))) & 0xff) << 8)
				| (this.af & 0xff);
	}

	private void adcAC(int[] asdf) { //0x89
		this.af = (((this.adcBytes((byte) ((this.af >> 8) & 0xff), (byte) ((this.bc) & 0xff))) & 0xff) << 8)
				| (this.af & 0xff);
	}

	private void adcAD(int[] asdf) { //0x8a
		this.af = (((this.adcBytes((byte) ((this.af >> 8) & 0xff), (byte) ((this.de >> 8) & 0xff))) & 0xff) << 8)
				| (this.af & 0xff);
	}

	private void adcAE(int[] asdf) { //0x8b
		this.af = (((this.adcBytes((byte) ((this.af >> 8) & 0xff), (byte) ((this.de) & 0xff))) & 0xff) << 8)
				| (this.af & 0xff);
	}

	private void adcAH(int[] asdf) { //0x8c
		this.af = (((this.adcBytes((byte) ((this.af >> 8) & 0xff), (byte) ((this.hl >> 8) & 0xff))) & 0xff) << 8)
				| (this.af & 0xff);
	}

	private void adcAL(int[] asdf) { //0x8d
		this.af = (((this.adcBytes((byte) ((this.af >> 8) & 0xff), (byte) ((this.hl) & 0xff))) & 0xff) << 8)
				| (this.af & 0xff);
	}

	private void adcAPtr(int[] asdf) { //0x8e
		this.af = (((this.adcBytes((byte) ((this.af >> 8) & 0xff), this.mem.read(this.hl))) & 0xff) << 8)
				| (this.af & 0xff);
	}

	private void adcAA(int[] asdf) { //0x8f
		this.af = (((this.adcBytes((byte) ((this.af >> 8) & 0xff), (byte) ((this.af >> 8) & 0xff))) & 0xff) << 8)
				| (this.af & 0xff);
	}

	//Subtract various registers
	private void subAB(int[] asdf) { //0x90
		this.af = (subBytes((byte) (this.af >> 8), (byte) ((this.bc >> 8) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void subAC(int[] asdf) { //0x91
		this.af = (subBytes((byte) (this.af >> 8), (byte) ((this.bc) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void subAD(int[] asdf) { //0x92
		this.af = (subBytes((byte) (this.af >> 8), (byte) ((this.de >> 8) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void subAE(int[] asdf) { //0x93
		this.af = (subBytes((byte) (this.af >> 8), (byte) ((this.de) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void subAH(int[] asdf) { //0x94
		this.af = (subBytes((byte) (this.af >> 8), (byte) ((this.hl >> 8) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void subAL(int[] asdf) { //0x95
		this.af = (subBytes((byte) (this.af >> 8), (byte) ((this.hl) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void subAPtr(int[] asdf) { //0x96
		this.af = (subBytes((byte) (this.af >> 8), this.mem.read(this.hl)) << 8) | (this.af & 0xff);
	}

	private void subAA(int[] asdf) { //0x93
		this.af = (subBytes((byte) (this.af >> 8), (byte) (this.af >> 8)) << 8) | (this.af & 0xff);
	}

	//Subtract with carry
	private void sbcAB(int[] asdf) { //0x98
		this.af = (sbcBytes((byte) (this.af >> 8), (byte) (this.bc >> 8)) << 8) | (this.af & 0xff);
	}

	private void sbcAC(int[] asdf) { //0x99
		this.af = (sbcBytes((byte) (this.af >> 8), (byte) ((this.bc) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void sbcAD(int[] asdf) { //0x9a
		this.af = (sbcBytes((byte) (this.af >> 8), (byte) ((this.de >> 8) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void sbcAE(int[] asdf) { //0x9b
		this.af = (sbcBytes((byte) (this.af >> 8), (byte) ((this.de) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void sbcAH(int[] asdf) { //0x9c
		this.af = (sbcBytes((byte) (this.af >> 8), (byte) ((this.hl >> 8) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void sbcAL(int[] asdf) { //0x9d
		this.af = (sbcBytes((byte) (this.af >> 8), (byte) ((this.hl) & 0xff)) << 8) | (this.af & 0xff);
	}

	private void sbcAPtr(int[] asdf) { //0x9e
		this.af = (sbcBytes((byte) (this.af >> 8), this.mem.read(this.hl)) << 8) | (this.af & 0xff);
	}

	private void sbcAA(int[] asdf) { //0x9f
		this.af = (sbcBytes((byte) (this.af >> 8), (byte) ((this.af >> 8) & 0xff)) << 8) | (this.af & 0xff);
	}

	//Bitwise and
	private void andBA(int[] asdf) { //0xa0
		and((this.bc >> 8) & 0xff);
	}

	private void andCA(int[] asdf) { //0xa1
		and(this.bc & 0xff);
	}

	private void andDA(int[] asdf) { //0xa2
		and((this.de >> 8) & 0xff);
	}

	private void andEA(int[] asdf) { //0xa3
		and(this.de & 0xff);
	}

	private void andHA(int[] asdf) { //0xa4
		and((this.hl >> 8) & 0xff);
	}

	private void andLA(int[] asdf) { //0xa5
		and(this.hl & 0xff);
	}

	private void andPtr(int[] asdf) { //0xa6
		this.and(this.mem.read(this.hl) & 0xff);
	}

	private void andAA(int[] asdf) { //0xa7
		and((this.af >> 8) & 0xff);
	}

	//Bitwise xor
	private void xorBA(int[] asdf) { //0xa8
		xor((this.bc >> 8) & 0xff);
	}

	private void xorCA(int[] asdf) { //0xa9
		xor(this.bc & 0xff);
	}

	private void xorDA(int[] asdf) { //0xaa
		xor((this.de >> 8) & 0xff);
	}

	private void xorEA(int[] asdf) { //0xab
		xor(this.de & 0xff);
	}

	private void xorHA(int[] asdf) { //0xac
		xor((this.hl >> 8) & 0xff);
	}

	private void xorLA(int[] asdf) { //0xad
		xor(this.hl & 0xff);
	}

	private void xorPtrA(int[] asdf) { //0xae
		xor(this.mem.read(this.hl) & 0xff);
	}

	private void xorAA(int[] asdf) { //0xaf
		xor((this.af >> 8) & 0xff);
	}

	//bitwise or
	private void orBA(int[] asdf) { //0xb0
		or((this.bc >> 8) & 0xff);
	}

	private void orCA(int[] asdf) { //0xb1
		or((this.bc) & 0xff);
	}

	private void orDA(int[] asdf) { //0xb2
		or((this.de >> 8) & 0xff);
	}

	private void orEA(int[] asdf) { //0xb3
		or((this.de) & 0xff);
	}

	private void orHA(int[] asdf) { //0xb4
		or((this.hl >> 8) & 0xff);
	}

	private void orLA(int[] asdf) { //0xb5
		or((this.hl) & 0xff);
	}

	private void orPtr(int[] asdf) { //0xb6
		this.or(this.mem.read(this.hl) & 0xff);
	}

	private void orAA(int[] asdf) { //0xb7
		or((this.af >> 8) & 0xff);
	}

	//Compare A with B
	private void cpb(int[] asdf) { //0xb8
		int a = this.af >> 8 & 0xff;
		int b = (this.bc >> 8) & 0xff;
		this.cp(a, b);
	}

	//Compare A with C
	private void cpc(int[] asdf) { //0xb9
		int a = this.af >> 8 & 0xff;
		int c = (this.bc) & 0xff;
		this.cp(a, c);
	}

	//Compare A with D
	private void cpd(int[] asdf) { //0xba
		int a = this.af >> 8 & 0xff;
		int d = (this.de >> 8) & 0xff;
		this.cp(a, d);
	}

	//Compare A with E
	private void cpe(int[] asdf) { //0xbb
		int a = this.af >> 8 & 0xff;
		int e = (this.de) & 0xff;
		this.cp(a, e);
	}

	//Compare A with H
	private void cph(int[] asdf) { //0xbc
		int a = this.af >> 8 & 0xff;
		int h = (this.hl >> 8) & 0xff;
		this.cp(a, h);
	}

	//Compare A with L
	private void cpL(int[] asdf) { //0xbd
		int a = this.af >> 8 & 0xff;
		int l = (this.hl) & 0xff;
		this.cp(a, l);
	}

	//Compare A with memory value
	private void cpPtr(int[] asdf) { //0xbe
		int a = this.af >> 8 & 0xff;
		int val = this.mem.read(this.hl) & 0xff;
		this.cp(a, val);
	}

	//Compare A with its self
	private void cpa(int[] asdf) { //0xbf
		int a = this.af >> 8 & 0xff;
		this.cp(a, a);
	}

	//Return of not zero
	private void retIfNotZero(int[] asdf) { //0xc0
		if ((this.af & FLAG_ZERO) == 0) {
			this.ret(asdf);
			this.c.inc(20);
		} else {
			this.c.inc(8);
		}
	}

	//Pop a short from the stack, store in BC
	private void popBC(int[] asdf) { //0xc1
		this.bc = this.popShort();
	}

	//Jump if not zero
	private void jumpNotZero(int[] off) { //0xc2
		if ((this.af & FLAG_ZERO) == 0) {
			this.pc = ((off[0] << 8) | off[1]) & 0xffff;
			this.c.inc(16);
		} else {
			this.c.inc(12);
		}
	}

	//Jump to address specified in next two bytes
	private void jmpi(int[] addr) { //0xc3
		this.pc = (addr[0] << 8 | addr[1]) & 0xffff;
	}

	//Call function if zero flag not set
	private void callIfNotZero(int[] args) { //0xc4
		if ((this.af & FLAG_ZERO) == 0) {
			this.call(args);
			this.c.inc(24);
		} else {
			this.c.inc(12);
		}
	}

	//Push BC onto stack
	private void pushBC(int[] asdf) { //0xc5
		this.pushShort((short) this.bc);
	}

	//Add register A with a constant
	private void addAN(int[] val) { //0c6
		this.af = (addBytes((byte) (this.af >> 8), (byte) val[0]) << 8) | (this.af & 0xff);
	}

	//Same as call 0
	private void rst0(int[] asdf) { //0xc7
		this.pushShort((short) this.pc);
		this.pc = 0;
	}

	//Returns if zero flag set
	private void retIfZero(int[] asdf) { //0xc8
		if ((this.af & FLAG_ZERO) != 0) {
			this.ret(asdf);
			this.c.inc(20);
		} else {
			this.c.inc(8);
		}
	}

	//Returns from function by popping a short from the stack and setting the program
	//counter to the popped value
	private void ret(int[] addr) { //0xc9
		this.pc = popShort() & 0xffff;
	}

	//Jump if zero flag set
	private void jumpZero(int[] off) { //0xca
		if ((this.af & FLAG_ZERO) != 0) {
			this.pc = ((off[0] << 8) | off[1]) & 0xffff;
			this.c.inc(16);
		} else {
			this.c.inc(12);
		}
	}

	//Call if zero flag set
	private void callIfZero(int[] args) { //0xcc
		if ((this.af & FLAG_ZERO) != 0) {
			this.call(args);
			this.c.inc(24);
		} else {
			this.c.inc(12);
		}
	}

	//Call a function by pushing the current value of PC onto the stack, then
	//sets the program counter to the next short in memory
	private void call(int[] args) { //0xcd
		short addr = (short) ((args[0] << 8) | args[1]);
		pushShort((short) this.pc);
		this.pc = addr & 0xffff;
	}

	//Add the next byte to register A with carry
	private void adcA(int[] n) { //0xce
		this.af = (((this.adcBytes((byte) ((this.af >> 8) & 0xff), (byte) n[0])) & 0xff) << 8) | (this.af & 0xff);
	}

	//Same as call 8
	private void rst8(int[] asdf) { //0xcf
		this.pushShort((short) this.pc);
		this.pc = 0x8;
	}

	//Returns if the carry flag is not set
	private void retIfNotCarry(int[] asdf) { //0xd0
		if ((this.af & FLAG_CARRY) == 0) {
			this.ret(asdf);
			this.c.inc(20);
		} else {
			this.c.inc(8);
		}
	}

	//Stores popped value from stack in DE
	private void popDE(int[] asdf) { //0xd1
		this.de = this.popShort();
	}

	//Jumps if the carry flag is not set
	private void jumpNotCarry(int[] off) { //0xd2
		if ((this.af & FLAG_CARRY) == 0) {
			this.pc = ((off[0] << 8) | off[1]) & 0xffff;
			this.c.inc(16);
		} else {
			this.c.inc(12);
		}
	}

	//Calls if the carry flag is not set
	private void callIfNotCarry(int[] args) { //0xd4
		if ((this.af & FLAG_CARRY) == 0) {
			this.call(args);
			this.c.inc(24);
		} else {
			this.c.inc(12);
		}
	}

	//Pushes DE onto the stack
	private void pushDE(int[] adsf) { //0xd5
		this.pushShort((short) this.de);
	}

	//Subtracts the next byte from A
	private void subA(int[] n) { //0xd6
		this.af = ((subBytes((byte) ((this.af >> 8) & 0xff), (byte) (n[0])) & 0xff) << 8) | (this.af & 0xff);
	}

	//Same as call 0x10
	private void rst10(int[] asdf) { //0xd7
		this.pushShort((short) this.pc);
		this.pc = 0x10;
	}

	//Returns if carry flag is set
	private void retIfCarry(int[] asdf) { //0xd8
		if ((this.af & FLAG_CARRY) > 0) {
			this.ret(asdf);
			this.c.inc(20);
		} else {
			this.c.inc(8);
		}
	}

	//Returns from an interrupt by enabling interrupts and returning as normal
	private void reti(int[] asdf) { //0xd9
		this.intsEnabled = true;
		this.inInterrupt = false;
		this.pc = this.popShort() & 0xffff;
	}

	//Jump if carry flag is set
	private void jumpCarry(int[] off) { //0xda
		if ((this.af & FLAG_CARRY) != 0) {
			this.pc = ((off[0] << 8) | off[1]) & 0xffff;
			this.c.inc(16);
		} else {
			this.c.inc(12);
		}
	}

	//Call function if carry flag is set
	private void callIfCarry(int[] args) { //0xdc
		if ((this.af & FLAG_CARRY) != 0) {
			this.call(args);
			this.c.inc(24);
		} else {
			this.c.inc(12);
		}
	}

	//Subtract next byte from A with carry
	private void sbcA(int[] n) { //0xde
		this.af = (sbcBytes((byte) (this.af >> 8), (byte) n[0]) << 8) | (this.af & 0xff);
	}

	//Same as call 0x18
	private void rst18(int[] asdf) { //0xdf
		this.pushShort((short) this.pc);
		this.pc = 0x18;
	}

	//Writes value in A to memory address 0xff00 + the next byte
	private void ldhNA(int[] off) { //0xe0
		this.mem.write(0xff00 | off[0], (byte) (this.af >> 8));
	}

	//Pops value from stack onto HL
	private void popHL(int[] asdf) { //0xe1
		this.hl = this.popShort() & 0xffff;
	}

	//Loads A into memory address 0xff00 + value in register C
	private void ldhCA(int[] asdf) { //0xe2
		this.mem.write(0xff00 | (this.bc & 0xff), (byte) (this.af >> 8));
	}

	//Push HL to stack
	private void pushHL(int[] asdf) { //0xe5
		this.pushShort((short) this.hl);
	}

	//And A with next byte
	private void andi(int[] val) { //0xe6
		and(val[0]);
	}

	//Same as call 0x20
	private void rst20(int[] asdf) { //0xe7
		this.pushShort((short) this.pc);
		this.pc = 0x20;
	}

	//Adds next byte to stack pointer
	private void addSpN(int[] n) { //0xe8
		this.sp = addShorts2((short) this.sp, (short) n[0]);
	}

	//Jumps to value stored in HL
	private void jmpHL(int[] asdf) { //0xe9
		this.pc = this.hl;
	}

	//Loads A into memory address stored in next short
	private void ldNA(int[] addr) { //0xea
		this.mem.write((addr[0] << 8) | (addr[1]), (byte) (this.af >> 8));
	}

	//Xors A with next byte
	private void xorNA(int[] n) { //0xe
		xor(n[0]);
	}

	//Same as call 0x28
	private void rst28(int[] asdf) { //0xef
		this.pushShort((short) this.pc);
		this.pc = 0x28;
	}

	//Loads value from 0xff00 + next byte into A
	private void ldhAN(int[] off) { //0xf0
		this.af = ((mem.read(0xff00 | (off[0] & 0xff)) & 0xff) << 8) | (this.af & 0xff);
	}

	//Pops from stack onto AF
	private void popAF(int[] asdf) { //0xf1
		this.af = this.popShort() & 0xfff0;
	}

	//Load value from memory address 0xff00 + register C, store in register A
	private void ldAUpper(int[] asdf) { //0xf2
		this.af = ((this.mem.read(0xff00 | (this.bc & 0xff)) << 8) & 0xff00) | (this.af & 0xff);
	}

	//Disable interrupts
	private void di(int[] asdf) { //0xf3
		//this.intsEnabled = false;
		this.queuedIntChange = true;
		this.queuedIntState = false;
	}

	//Push AF onto stack
	private void pushAF(int[] asdf) { //0xf5
		this.pushShort((short) this.af);
	}

	//Ors next byte with register A
	private void orN(int[] val) { //0xf6
		this.or(val[0]);
	}

	//Same as call 0x30
	private void rst30(int[] asdf) { //0xf7
		this.pushShort((short) this.pc);
		this.pc = 0x30;
	}

	//Add stack pointer and next byte, store in HL
	private void ldHlSpN(int[] n) { //0xf8
		this.hl = this.addShorts2((short) this.sp, (short) (n[0] & 0xff)) & 0xffff;
	}

	//Load HL into stack pointer
	private void ldSpHl(int[] asdf) { //0xf9
		this.sp = this.hl;
	}

	//Loads next value stored in the location specified by next 2 bytes into A
	private void ldPtrA(int[] ptr) { //0xfa
		this.af = (this.af & 0xff) | ((this.mem.read(((ptr[0] << 8) | ptr[1])) & 0xff) << 8);
	}

	//Enable interrupts
	private void ei(int[] asdf) { //0xfb
		//this.intsEnabled = true;
		this.queuedIntChange = true;
		this.queuedIntState = true;
	}

	//Compare A with next byte
	private void cpn(int[] n) { //0xfe
		int a = this.af >> 8 & 0xff;
		int b = n[0];
		this.cp(a, b);
	}

	//Same as call 0x38
	private void rst38(int[] asdf) { //0xff
		this.pushShort((short) this.pc);
		this.pc = 0x38;
	}

	/*
	 * CB instruction extensions
	 */

	//Rotate left with carry
	private void rlcB(int[] asdf) { //0xcb00
		this.bc = ((this.rlc((byte) ((this.bc >> 8) & 0xff)) & 0xff) << 8) | (this.bc & 0xff);
	}

	private void rlcC(int[] asdf) { //0xcb01
		this.bc = ((this.rlc((byte) ((this.bc) & 0xff)) & 0xff)) | (this.bc & 0xff00);
	}

	private void rlcD(int[] asdf) { //0xcb02
		this.de = ((this.rlc((byte) ((this.de >> 8) & 0xff)) & 0xff) << 8) | (this.de & 0xff);
	}

	private void rlcE(int[] asdf) { //0xcb03
		this.de = ((this.rlc((byte) ((this.de) & 0xff)) & 0xff)) | (this.de & 0xff00);
	}

	private void rlcH(int[] asdf) { //0xcb04
		this.hl = ((this.rlc((byte) ((this.hl >> 8) & 0xff)) & 0xff) << 8) | (this.hl & 0xff);
	}

	private void rlcL(int[] asdf) { //0xcb05
		this.hl = ((this.rlc((byte) ((this.hl) & 0xff)) & 0xff)) | (this.hl & 0xff00);
	}

	private void rlcPtr(int[] asdf) { //0xcb06
		this.mem.write(this.hl, (byte) this.rlc(this.mem.read(this.hl)));
	}

	private void rlcA(int[] asdf) { //0xcb07
		this.af = ((this.rlc((byte) ((this.af >> 8) & 0xff)) & 0xff) << 8) | (this.af & 0xff);
	}

	//Rotate right with carry
	private void rrcB(int[] asdf) { //0xcb08
		this.bc = (this.bc & 0xff) | ((this.rrc((byte) ((this.bc >> 8) & 0xff)) & 0xff) << 8);
	}

	private void rrcC(int[] asdf) { //0xcb09
		this.bc = (this.bc & 0xff00) | (this.rrc((byte) (this.bc & 0xff)) & 0xff);
	}

	private void rrcD(int[] asdf) { //0xcb0a
		this.de = (this.de & 0xff) | ((this.rrc((byte) ((this.de >> 8) & 0xff)) & 0xff) << 8);
	}

	private void rrcE(int[] asdf) { //0xcb0b
		this.de = (this.de & 0xff00) | (this.rrc((byte) (this.de & 0xff)) & 0xff);
	}

	private void rrcH(int[] asdf) { //0xcb0c
		this.hl = (this.hl & 0xff) | ((this.rrc((byte) ((this.hl >> 8) & 0xff)) & 0xff) << 8);
	}

	private void rrcL(int[] asdf) { //0xcb0d
		this.hl = (this.hl & 0xff00) | ((this.rrc((byte) ((this.hl) & 0xff)) & 0xff));
	}

	private void rrcPtr(int[] asdf) { //0xcb0e
		this.mem.write(this.hl, (byte) this.rrc(this.mem.read(this.hl)));
	}

	private void rrcA(int[] asdf) { //0xcb0f
		this.af = ((this.rrc((byte) ((this.af >> 8) & 0xff)) & 0xff) << 8) | (this.af & 0xff);
	}

	//Rotate left
	private void rlB(int[] asdf) { //0xcb10
		this.bc = (this.bc & 0xff) | (this.rl((byte) ((this.bc >> 8) & 0xff)) << 8);
	}

	private void rlC(int[] asdf) { //0xcb11
		this.bc = (this.bc & 0xff00) | this.rl((byte) ((this.bc) & 0xff));
	}

	private void rlD(int[] asdf) { //0xcb12
		this.de = (this.de & 0xff) | ((this.rl((byte) ((this.de >> 8) & 0xff)) << 8) & 0xff00);
	}

	private void rlE(int[] asdf) { //0xcb13
		this.de = (this.de & 0xff00) | this.rl((byte) ((this.de) & 0xff));
	}

	private void rlH(int[] asdf) { //0xcb14
		this.hl = (this.hl & 0xff) | ((this.rl((byte) ((this.hl >> 8) & 0xff)) << 8) & 0xff00);
	}

	private void rlL(int[] asdf) { //0xcb15
		this.hl = (this.hl & 0xff00) | this.rl((byte) ((this.hl) & 0xff));
	}

	private void rlPtr(int[] asdf) { //0xcb16
		this.mem.write(this.hl, (byte) this.rl(this.mem.read(this.hl)));
	}

	private void rlA(int[] asdf) { //0xcb17
		this.af = ((this.rl((byte) ((this.af >> 8) & 0xff)) << 8) & 0xff00) | (this.af & 0xff);
	}

	//Rotate right
	private void rrb(int[] asdf) { //0xcb18
		this.bc = ((this.rr((byte) (this.bc >> 8)) & 0xff) << 8) | (this.bc & 0xff);
	}

	private void rrc(int[] asdf) { //0xcb19
		this.bc = ((this.rr((byte) (this.bc & 0xff)) & 0xff)) | (this.bc & 0xff00);
	}

	private void rrd(int[] asdf) { //0xcb1a
		this.de = ((this.rr((byte) (this.de >> 8)) & 0xff) << 8) | (this.de & 0xff);
	}

	private void rre(int[] asdf) { //0xcb1b
		this.de = ((this.rr((byte) (this.de & 0xff)) & 0xff)) | (this.de & 0xff00);
	}

	private void rrh(int[] asdf) { //0xcb1c
		this.hl = ((this.rr((byte) (this.hl >> 8)) & 0xff) << 8) | (this.hl & 0xff);
	}

	private void rrl(int[] asdf) { //0xcb1d
		this.hl = ((this.rr((byte) (this.hl & 0xff)) & 0xff)) | (this.hl & 0xff00);
	}

	private void rrptr(int[] asdf) { //0xcb1e
		this.mem.write(this.hl, (byte) this.rr(this.mem.read(this.hl)));
	}

	private void rra(int[] asdf) { //0xcb1f
		this.af = ((this.rr((byte) (this.af >> 8)) & 0xff) << 8) | (this.af & 0xff);
	}

	//Arithmetic shift left
	private void slaB(int[] asdf) { //0xcb20
		this.bc = (this.bc & 0xff) | ((this.sla((byte) ((this.bc >> 8) & 0xff)) & 0xff) << 8);
	}

	private void slaC(int[] asdf) { //0xcb21
		this.bc = (this.bc & 0xff00) | (this.sla((byte) (this.bc & 0xff)) & 0xff);
	}

	private void slaD(int[] asdf) { //0xcb22
		this.de = (this.de & 0xff) | ((this.sla((byte) ((this.de >> 8) & 0xff)) & 0xff) << 8);
	}

	private void slaE(int[] asdf) { //0xcb23
		this.de = (this.de & 0xff00) | (this.sla((byte) (this.de & 0xff)) & 0xff);
	}

	private void slaH(int[] asdf) { //0xcb24
		this.hl = (this.hl & 0xff) | ((this.sla((byte) ((this.hl >> 8) & 0xff)) & 0xff) << 8);
	}

	private void slaL(int[] asdf) { //0xcb25
		this.hl = (this.hl & 0xff00) | (this.sla((byte) (this.hl & 0xff)) & 0xff);
	}

	private void slaPtr(int[] asdf) { //0xcb26
		this.mem.write(this.hl, (byte) this.sla(this.mem.read(this.hl)));
	}

	private void slaA(int[] asdf) { //0xcb27
		this.af = ((this.sla((byte) (this.af >> 8)) & 0xff) << 8) | (this.af & 0xff);
	}

	//Arithmetic shift right
	private void sraB(int[] asdf) { //0xcb28
		this.bc = (this.bc & 0xff) | (this.sra((byte) ((this.bc >> 8) & 0xff)) << 8);
	}

	private void sraC(int[] asdf) { //0xcb29
		this.bc = (this.bc & 0xff00) | (this.sra((byte) ((this.bc) & 0xff)));
	}

	private void sraD(int[] asdf) { //0xcb2a
		this.de = (this.de & 0xff) | (this.sra((byte) ((this.de >> 8) & 0xff)) << 8);
	}

	private void sraE(int[] asdf) { //0xcb2b
		this.de = (this.de & 0xff00) | (this.sra((byte) ((this.de) & 0xff)));
	}

	private void sraH(int[] asdf) { //0xcb2c
		this.hl = (this.hl & 0xff) | (this.sra((byte) ((this.hl >> 8) & 0xff)) << 8);
	}

	private void sraL(int[] asdf) { //0xcb2d
		this.hl = (this.hl & 0xff00) | (this.sra((byte) ((this.hl) & 0xff)));
	}

	private void sraPtr(int[] asdf) { //0xcb2e
		this.mem.write(this.hl, (byte) this.sra(this.mem.read(this.hl)));
	}

	private void sraA(int[] asdf) { //0xcb2f
		this.af = (this.sra((byte) ((this.af >> 8) & 0xff)) << 8) | (this.af & 0xff);
	}

	//Swap nibbles
	private void swapB(int[] asdf) { //0xcb30
		this.bc = ((swap((byte) ((this.bc >> 8) & 0xff)) & 0xff) << 8) | (this.bc & 0xff);
	}

	private void swapC(int[] asdf) { //0xcb31
		this.bc = (swap((byte) (this.bc & 0xff)) & 0xff) | (this.bc & 0xff00);
	}

	private void swapD(int[] asdf) { //0xcb32
		this.de = ((swap((byte) ((this.de >> 8) & 0xff)) & 0xff) << 8) | (this.de & 0xff);
	}

	private void swapE(int[] asdf) { //0xcb33
		this.de = (swap((byte) (this.de & 0xff)) & 0xff) | (this.de & 0xff00);
	}

	private void swapH(int[] asdf) { //0xcb34
		this.hl = ((swap((byte) ((this.hl >> 8) & 0xff)) & 0xff) << 8) | (this.hl & 0xff);
	}

	private void swapL(int[] asdf) { //0xcb35
		this.hl = (swap((byte) (this.hl & 0xff)) & 0xff) | (this.hl & 0xff00);
	}

	private void swapPtr(int[] asdf) { //0xcb36
		this.mem.write(this.hl, (byte) (swap(this.mem.read(this.hl)) & 0xff));
	}

	private void swapA(int[] asdf) { //0xcb37
		this.af = ((swap((byte) (this.af >> 8)) & 0xff) << 8) | (this.af & 0xff);
	}

	//Logical shift right
	private void srlB(int[] asdf) { //0xcb39
		this.bc = (this.srl((byte) (this.bc >> 8)) << 8) | (this.bc & 0xff);
	}

	private void srlC(int[] asdf) { //0xcb39
		this.bc = (this.srl((byte) (this.bc))) | (this.bc & 0xff00);
	}

	private void srlD(int[] asdf) { //0xcb3a
		this.de = (this.srl((byte) (this.de >> 8)) << 8) | (this.de & 0xff);
	}

	private void srlE(int[] asdf) { //0xcb3b
		this.de = (this.srl((byte) (this.de))) | (this.de & 0xff00);
	}

	private void srlH(int[] asdf) { //0xcb3c
		this.hl = (this.srl((byte) (this.hl >> 8)) << 8) | (this.hl & 0xff);
	}

	private void srlL(int[] asdf) { //0xcb3d
		this.hl = (this.srl((byte) (this.hl))) | (this.hl & 0xff00);
	}

	private void srlPtr(int[] asdf) { //0xcb3e
		this.mem.write(this.hl, (byte) this.srl(this.mem.read(this.hl)));
	}

	private void srlA(int[] asdf) { //0xcb3f
		this.af = (this.srl((byte) (this.af >> 8)) << 8) | (this.af & 0xff);
	}

	//Check if bits are set
	private void bit0B(int[] asdf) { //0xcb40
		bit((byte) 1, (byte) (this.bc >> 8));
	}

	private void bit0C(int[] asdf) { //0xcb41
		bit((byte) 1, (byte) (this.bc & 0xff));
	}

	private void bit0D(int[] asdf) { //0xcb42
		bit((byte) 1, (byte) ((this.de >> 8) & 0xff));
	}

	private void bit0E(int[] asdf) { //0xcb43
		bit((byte) 1, (byte) (this.de & 0xff));
	}

	private void bit0H(int[] asdf) { //0xcb44
		bit((byte) 1, (byte) ((this.hl >> 8) & 0xff));
	}

	private void bit0L(int[] asdf) { //0xcb45
		bit((byte) 1, (byte) ((this.hl) & 0xff));
	}

	private void bit0HL(int[] asdf) { //0xcb46
		bit((byte) 1, this.mem.read(this.hl));
	}

	private void bit0A(int[] asdf) { //0xcb47
		bit((byte) 1, (byte) (this.af >> 8));
	}

	private void bit1B(int[] asdf) { //0xcb48
		bit((byte) 2, (byte) (this.bc >> 8));
	}

	private void bit1C(int[] asdf) { //0xcb49
		bit((byte) 2, (byte) (this.bc));
	}

	private void bit1D(int[] asdf) { //0xcb4a
		bit((byte) 2, (byte) (this.de >> 8));
	}

	private void bit1E(int[] asdf) { //0xcb4b
		bit((byte) 2, (byte) (this.de));
	}

	private void bit1H(int[] asdf) { //0xcb4c
		bit((byte) 2, (byte) (this.hl >> 8));
	}

	private void bit1L(int[] asdf) { //0xcb4d
		bit((byte) 2, (byte) (this.hl));
	}

	private void bit1HL(int[] asdf) { //0xcb4e
		bit((byte) 2, this.mem.read(this.hl));
	}

	private void bit1A(int[] asdf) { //0xcb4f
		bit((byte) 2, (byte) (this.af >> 8));
	}

	private void bit2B(int[] asdf) { //0xcb50
		bit((byte) 4, (byte) (this.bc >> 8));
	}

	private void bit2C(int[] asdf) { //0xcb51
		bit((byte) 4, (byte) (this.bc));
	}

	private void bit2D(int[] asdf) { //0xcb52
		bit((byte) 4, (byte) (this.de >> 8));
	}

	private void bit2E(int[] asdf) { //0xcb53
		bit((byte) 4, (byte) (this.de));
	}

	private void bit2H(int[] asdf) { //0xcb54
		bit((byte) 4, (byte) (this.hl >> 8));
	}

	private void bit2L(int[] asdf) { //0xcb55
		bit((byte) 4, (byte) (this.hl));
	}

	private void bit2HL(int[] asdf) { //0xcb56
		bit((byte) 4, this.mem.read(this.hl));
	}

	private void bit2A(int[] asdf) { //0xcb57
		bit((byte) 4, (byte) (this.af >> 8));
	}

	private void bit3B(int[] asdf) { //0xcb58
		bit((byte) 8, (byte) (this.bc >> 8));
	}

	private void bit3C(int[] asdf) { //0xcb59
		bit((byte) 8, (byte) (this.bc));
	}

	private void bit3D(int[] asdf) { //0xcb5a
		bit((byte) 8, (byte) (this.de >> 8));
	}

	private void bit3E(int[] asdf) { //0xcb5b
		bit((byte) 8, (byte) (this.de));
	}

	private void bit3H(int[] asdf) { //0xcb5c
		bit((byte) 8, (byte) (this.hl >> 8));
	}

	private void bit3L(int[] asdf) { //0xcb5d
		bit((byte) 8, (byte) (this.hl));
	}

	private void bit3HL(int[] asdf) { //0xcb5e
		bit((byte) (1 << 3), this.mem.read(this.hl));
	}

	private void bit3A(int[] asdf) { //0xcb5f
		bit((byte) 8, (byte) (this.af >> 8));
	}

	private void bit4B(int[] asdf) { //0xcb60
		bit((byte) (1 << 4), (byte) (this.bc >> 8));
	}

	private void bit4C(int[] asdf) { //0xcb61
		bit((byte) (1 << 4), (byte) (this.bc & 0xff));
	}

	private void bit4D(int[] asdf) { //0xcb62
		bit((byte) (1 << 4), (byte) ((this.de >> 8) & 0xff));
	}

	private void bit4E(int[] asdf) { //0xcb63
		bit((byte) (1 << 4), (byte) (this.de & 0xff));
	}

	private void bit4H(int[] asdf) { //0xcb64
		bit((byte) (1 << 4), (byte) ((this.hl >> 8) & 0xff));
	}

	private void bit4L(int[] asdf) { //0xcb65
		bit((byte) (1 << 4), (byte) (this.hl & 0xff));
	}

	private void bit4HL(int[] asdf) { //0xcb66
		bit((byte) (1 << 4), this.mem.read(this.hl));
	}

	private void bit4A(int[] asdf) { //0xcb67
		bit((byte) (1 << 4), (byte) (this.af >> 8));
	}

	private void bit5B(int[] asdf) { //0xcb68
		bit((byte) (1 << 5), (byte) (this.bc >> 8));
	}

	private void bit5C(int[] asdf) { //0xcb69
		bit((byte) (1 << 5), (byte) (this.bc & 0xff));
	}

	private void bit5D(int[] asdf) { //0xcb6a
		bit((byte) (1 << 5), (byte) ((this.de >> 8) & 0xff));
	}

	private void bit5E(int[] asdf) { //0xcb6b
		bit((byte) (1 << 5), (byte) ((this.de) & 0xff));
	}

	private void bit5H(int[] asdf) { //0xcb6c
		bit((byte) (1 << 5), (byte) ((this.hl >> 8) & 0xff));
	}

	private void bit5L(int[] asdf) { //0xcb6d
		bit((byte) (1 << 5), (byte) ((this.hl) & 0xff));
	}

	private void bit5HL(int[] asdf) { //0xcb6e
		bit((byte) (1 << 5), this.mem.read(this.hl));
	}

	private void bit5A(int[] asdf) { //0xcb6f
		bit((byte) (1 << 5), (byte) (this.af >> 8));
	}

	private void bit6B(int[] asdf) { //0xcb70
		bit((byte) (1 << 6), (byte) (this.bc >> 8));
	}

	private void bit6C(int[] asdf) { //0xcb71
		bit((byte) (1 << 6), (byte) (this.bc));
	}

	private void bit6D(int[] asdf) { //0xcb72
		bit((byte) (1 << 6), (byte) (this.de >> 8));
	}

	private void bit6E(int[] asdf) { //0xcb73
		bit((byte) (1 << 6), (byte) (this.de));
	}

	private void bit6H(int[] asdf) { //0xcb74
		bit((byte) (1 << 6), (byte) (this.hl >> 8));
	}

	private void bit6L(int[] asdf) { //0xcb75
		bit((byte) (1 << 6), (byte) (this.hl));
	}

	private void bit6HL(int[] asdf) { //0xcb76
		bit((byte) (1 << 6), this.mem.read(this.hl));
	}

	private void bit6A(int[] asdf) { //0xcb77
		bit((byte) (1 << 6), (byte) (this.af >> 8));
	}

	private void bit7B(int[] asdf) { //0xcb78
		bit((byte) (1 << 7), (byte) (this.bc >> 8));
	}

	private void bit7C(int[] asdf) { //0xcb7a
		bit((byte) (1 << 7), (byte) (this.bc));
	}

	private void bit7D(int[] asdf) { //0xcb7a
		bit((byte) (1 << 7), (byte) (this.de >> 8));
	}

	private void bit7E(int[] asdf) { //0xcb7b
		bit((byte) (1 << 7), (byte) (this.de));
	}

	private void bit7H(int[] asdf) { //0xcb7c
		bit((byte) (1 << 7), (byte) (this.hl >> 8));
	}

	private void bit7L(int[] asdf) { //0xcb7d
		bit((byte) (1 << 7), (byte) (this.hl));
	}

	private void bit7HL(int[] asdf) { //0xcb7e
		bit((byte) (1 << 7), this.mem.read(this.hl));
	}

	private void bit7A(int[] asdf) { //0xcb7f
		bit((byte) (1 << 7), (byte) (this.af >> 8));
	}

	//Set certain bits to 0
	private void res0b(int[] asdf) { //0xcb80
		this.bc &= ~((1 << 0) << 8);
	}

	private void res0c(int[] asdf) { //0xcb81
		this.bc &= ~(1 << 0);
	}

	private void res0d(int[] asdf) { //0xcb82
		this.de &= ~((1 << 0) << 8);
	}

	private void res0e(int[] asdf) { //0xcb83
		this.de &= ~(1 << 0);
	}

	private void res0h(int[] asdf) { //0xcb84
		this.hl &= ~((1 << 0) << 8);
	}

	private void res0l(int[] asdf) { //0xcb85
		this.hl &= ~(1 << 0);
	}

	private void res0Ptr(int[] asdf) { //0xcb86
		this.mem.write(this.hl, (byte) (this.mem.read(this.hl) & ~(1 << 0)));
	}

	private void res0a(int[] asdf) { //0xcb87
		resa(0);
	}

	private void res1b(int[] asdf) { // 0xcb88
		this.bc &= ~((1 << 1) << 8);
	}

	private void res1c(int[] asdf) { // 0xcb89
		this.bc &= ~(1 << 1);
	}

	private void res1d(int[] asdf) { // 0xcb8a
		this.de &= ~((1 << 1) << 8);
	}

	private void res1e(int[] asdf) { // 0xcb8b
		this.de &= ~(1 << 1);
	}

	private void res1h(int[] asdf) { // 0xcb8c
		this.hl &= ~((1 << 1) << 8);
	}

	private void res1l(int[] asdf) { // 0xcb8d
		this.hl &= ~(1 << 1);
	}

	private void res1Ptr(int[] asdf) { //0xcb8e
		this.mem.write(this.hl, (byte) (this.mem.read(this.hl) & ~(2)));
	}

	private void res1a(int[] asdf) { // 0xcb8f
		resa(1);
	}

	private void res2b(int[] asdf) { // 0xcb90
		this.bc &= ~((1 << 2) << 8);
	}

	private void res2c(int[] asdf) { // 0xcb91
		this.bc &= ~(1 << 2);
	}

	private void res2d(int[] asdf) { // 0xcb92
		this.de &= ~((1 << 2) << 8);
	}

	private void res2e(int[] asdf) { // 0xcb93
		this.de &= ~(1 << 2);
	}

	private void res2h(int[] asdf) { // 0xcb94
		this.hl &= ~((1 << 2) << 8);
	}

	private void res2l(int[] asdf) { // 0xcb95
		this.hl &= ~(1 << 2);
	}

	private void res2Ptr(int[] asdf) { //0xcb96
		this.mem.write(this.hl, (byte) (this.mem.read(this.hl) & ~(1 << 2)));
	}

	private void res2a(int[] asdf) { // 0xcb97
		resa(2);
	}

	private void res3b(int[] asdf) { // 0xcb98
		this.bc &= ~((1 << 3) << 8);
	}

	private void res3c(int[] asdf) { // 0xcb99
		this.bc &= ~(1 << 3);
	}

	private void res3d(int[] asdf) { // 0xcb9a
		this.de &= ~((1 << 3) << 8);
	}

	private void res3e(int[] asdf) { // 0xcb9b
		this.de &= ~(1 << 3);
	}

	private void res3h(int[] asdf) { // 0xcb9c
		this.hl &= ~((1 << 3) << 8);
	}

	private void res3l(int[] asdf) { // 0xcb9d
		this.hl &= ~(1 << 3);
	}

	private void res3Ptr(int[] asdf) { //0xcb9e
		this.mem.write(this.hl, (byte) (this.mem.read(this.hl) & ~(1 << 3)));
	}

	private void res3a(int[] asdf) { // 0xcb9f
		this.af &= ~((1 << 3) << 8);
	}

	private void res4b(int[] asdf) { // 0xcba0
		this.bc &= ~((1 << 4) << 8);
	}

	private void res4c(int[] asdf) { // 0xcba1
		this.bc &= ~(1 << 4);
	}

	private void res4d(int[] asdf) { // 0xcba2
		this.de &= ~((1 << 4) << 8);
	}

	private void res4e(int[] asdf) { // 0xcba3
		this.de &= ~(1 << 4);
	}

	private void res4h(int[] asdf) { // 0xcba4
		this.hl &= ~((1 << 4) << 8);
	}

	private void res4l(int[] asdf) { // 0xcba5
		this.hl &= ~(1 << 4);
	}

	private void res4Ptr(int[] asdf) { //0xcba6
		this.mem.write(this.hl, (byte) (this.mem.read(this.hl) & ~(1 << 4)));
	}

	private void res4a(int[] asdf) { // 0xcba7
		this.af &= ~((1 << 4) << 8);
	}

	private void res5b(int[] asdf) { // 0xcba8
		this.bc &= ~((1 << 5) << 8);
	}

	private void res5c(int[] asdf) { // 0xcba9
		this.bc &= ~(1 << 5);
	}

	private void res5d(int[] asdf) { // 0xcbaa
		this.de &= ~((1 << 5) << 8);
	}

	private void res5e(int[] asdf) { // 0xcbab
		this.de &= ~((1 << 5));
	}

	private void res5h(int[] asdf) { // 0xcba8
		this.hl &= ~((1 << 5) << 8);
	}

	private void res5l(int[] asdf) { // 0xcba8
		this.hl &= ~((1 << 5));
	}

	private void res5Ptr(int[] asdf) { //0xcbae
		this.mem.write(this.hl, (byte) (this.mem.read(this.hl) & ~(1 << 5)));
	}

	private void res5a(int[] asdf) { // 0xcbaf
		resa(5);
	}

	private void res6b(int[] asdf) { // 0xcbb0
		this.bc &= ~((1 << 6) << 8);
	}

	private void res6c(int[] asdf) { // 0xcbb1
		this.bc &= ~(1 << 6);
	}

	private void res6d(int[] asdf) { // 0xcbb2
		this.de &= ~((1 << 6) << 8);
	}

	private void res6e(int[] asdf) { // 0xcbb3
		this.de &= ~(1 << 6);
	}

	private void res6h(int[] asdf) { // 0xcbb4
		this.hl &= ~((1 << 6) << 8);
	}

	private void res6l(int[] asdf) { // 0xcbb5
		this.hl &= ~(1 << 6);
	}

	private void res6Ptr(int[] asdf) { //0xcbb6
		this.mem.write(this.hl, (byte) (this.mem.read(this.hl) & ~(1 << 6)));
	}

	private void res6a(int[] asdf) { // 0xcbb7
		this.af &= ~((1 << 6) << 8);
	}

	private void res7b(int[] asdf) { // 0xcbb8
		this.bc &= ~((1 << 7) << 8);
	}

	private void res7c(int[] asdf) { // 0xcbba
		this.bc &= ~((1 << 7));
	}

	private void res7d(int[] asdf) { // 0xcbba
		this.de &= ~((1 << 7) << 8);
	}

	private void res7e(int[] asdf) { // 0xcbbb
		this.de &= ~((1 << 7));
	}

	private void res7h(int[] asdf) { // 0xcbbc
		this.hl &= ~((1 << 7) << 8);
	}

	private void res7l(int[] asdf) { // 0xcbbd
		this.hl &= ~((1 << 7));
	}

	private void res7Ptr(int[] asdf) { //0xcbbe
		this.mem.write(this.hl, (byte) (this.mem.read(this.hl) & ~(1 << 7)));
	}

	private void res7a(int[] asdf) { // 0xcbbf
		resa(7);
	}

	//Set certain bits to 0
	private void set0B(int[] asdf) { //0xcbc0
		this.bc |= ((1 << 0) << 8);
	}

	private void set0C(int[] asdf) { //0xcbc1
		this.bc |= (1 << 0);
	}

	private void set0D(int[] asdf) { //0xcbc2
		this.de |= ((1 << 0) << 8);
	}

	private void set0E(int[] asdf) { //0xcbc3
		this.de |= (1 << 0);
	}

	private void set0H(int[] asdf) { //0xcbc4
		this.hl |= ((1 << 0) << 8);
	}

	private void set0L(int[] asdf) { //0xcbc5
		this.hl |= (1 << 0);
	}

	private void set0Ptr(int[] asdf) { //0xcbc6
		this.mem.write(this.hl, (byte) (this.mem.read(hl) | (1)));
	}

	private void set0A(int[] asdf) { //0xcbc7
		this.af |= (1 << 0) << 8;
	}

	private void set1B(int[] asdf) { //0xcbc8
		this.bc |= (1 << 1) << 8;
	}

	private void set1C(int[] asdf) { //0xcbc9
		this.bc |= (1 << 1);
	}

	private void set1D(int[] asdf) { //0xcbca
		this.de |= (1 << 1) << 8;
	}

	private void set1E(int[] asdf) { //0xcbcb
		this.de |= (1 << 1);
	}

	private void set1H(int[] asdf) { //0xcbcc
		this.hl |= (1 << 1) << 8;
	}

	private void set1L(int[] asdf) { //0xcbcd
		this.hl |= (1 << 1);
	}

	private void set1Ptr(int[] asdf) { //0xcbce
		this.mem.write(this.hl, (byte) (this.mem.read(hl) | (2)));
	}

	private void set1A(int[] asdf) { //0xcbcf
		this.af |= (1 << 1) << 8;
	}

	private void set2B(int[] asdf) { //0xcbd0
		this.bc |= (1 << 2) << 8;
	}

	private void set2C(int[] asdf) { //0xcbd1
		this.bc |= (1 << 2);
	}

	private void set2D(int[] asdf) { //0xcbd2
		this.de |= (1 << 2) << 8;
	}

	private void set2E(int[] asdf) { //0xcbd3
		this.de |= (1 << 2);
	}

	private void set2H(int[] asdf) { //0xcbd4
		this.hl |= (1 << 2) << 8;
	}

	private void set2L(int[] asdf) { //0xcbd5
		this.hl |= (1 << 2);
	}

	private void set2Ptr(int[] asdf) { //0xcbd6
		this.mem.write(this.hl, (byte) (this.mem.read(hl) | (1 << 2)));
	}

	private void set2A(int[] asdf) { //0xcbd7
		this.af |= (1 << 2) << 8;
	}

	private void set3B(int[] asdf) { //0xcbd8
		this.bc |= (1 << 3) << 8;
	}

	private void set3C(int[] asdf) { //0xcbd9
		this.bc |= (1 << 3);
	}

	private void set3D(int[] asdf) { //0xcbda
		this.de |= (1 << 3) << 8;
	}

	private void set3E(int[] asdf) { //0xcbdb
		this.de |= (1 << 3);
	}

	private void set3H(int[] asdf) { //0xcbdc
		this.hl |= (1 << 3) << 8;
	}

	private void set3L(int[] asdf) { //0xcbdd
		this.hl |= (1 << 3);
	}

	private void set3Ptr(int[] asdf) { //0xcbde
		this.mem.write(this.hl, (byte) (this.mem.read(hl) | (1 << 3)));
	}

	private void set3A(int[] asdf) { //0xcbdf
		this.af |= (1 << 3) << 8;
	}

	private void set4B(int[] asdf) { //0xcbe0
		this.bc |= (1 << 4) << 8;
	}

	private void set4C(int[] asdf) { //0xcbe1
		this.bc |= (1 << 4);
	}

	private void set4D(int[] asdf) { //0xcbe2
		this.de |= (1 << 4) << 8;
	}

	private void set4E(int[] asdf) { //0xcbe3
		this.de |= (1 << 4);
	}

	private void set4H(int[] asdf) { //0xcbe4
		this.hl |= (1 << 4) << 8;
	}

	private void set4L(int[] asdf) { //0xcbe5
		this.hl |= (1 << 4);
	}

	private void set4Ptr(int[] asdf) { //0xcbe6
		this.mem.write(this.hl, (byte) (this.mem.read(hl) | (1 << 4)));
	}

	private void set4A(int[] asdf) { //0xcbe7
		this.af |= (1 << 4) << 8;
	}

	private void set5B(int[] asdf) { //0xcbe8
		this.bc |= (1 << 5) << 8;
	}

	private void set5C(int[] asdf) { //0xcbe9
		this.bc |= (1 << 5);
	}

	private void set5D(int[] asdf) { //0xcbea
		this.de |= (1 << 5) << 8;
	}

	private void set5E(int[] asdf) { //0xcbeb
		this.de |= (1 << 5);
	}

	private void set5H(int[] asdf) { //0xcbec
		this.hl |= (1 << 5) << 8;
	}

	private void set5L(int[] asdf) { //0xcbed
		this.hl |= (1 << 5);
	}

	private void set5Ptr(int[] asdf) { //0xcbee
		this.mem.write(this.hl, (byte) (this.mem.read(hl) | (1 << 5)));
	}

	private void set5A(int[] asdf) { //0xcbef
		this.af |= (1 << 5) << 8;
	}

	private void set6B(int[] asdf) { //0xcbf0
		this.bc |= (1 << 6) << 8;
	}

	private void set6C(int[] asdf) { //0xcbf1
		this.bc |= (1 << 6);
	}

	private void set6D(int[] asdf) { //0xcbf2
		this.de |= (1 << 6) << 8;
	}

	private void set6E(int[] asdf) { //0xcbf3
		this.de |= (1 << 6);
	}

	private void set6H(int[] asdf) { //0xcbf4
		this.hl |= (1 << 6) << 8;
	}

	private void set6L(int[] asdf) { //0xcbf5
		this.hl |= (1 << 6);
	}

	private void set6Ptr(int[] asdf) { //0xcbf6
		this.mem.write(this.hl, (byte) (this.mem.read(hl) | (1 << 6)));
	}

	private void set6A(int[] asdf) { //0xcbf7
		this.af |= (1 << 6) << 8;
	}

	private void set7B(int[] asdf) { //0xcbf8
		this.bc |= (1 << 7) << 8;
	}

	private void set7C(int[] asdf) { //0xcbf9
		this.bc |= (1 << 7);
	}

	private void set7D(int[] asdf) { //0xcbfa
		this.de |= (1 << 7) << 8;
	}

	private void set7E(int[] asdf) { //0xcbfb
		this.de |= (1 << 7);
	}

	private void set7H(int[] asdf) { //0xcbfc
		this.hl |= (1 << 7) << 8;
	}

	private void set7L(int[] asdf) { //0xcbfd
		this.hl |= (1 << 7);
	}

	private void set7Ptr(int[] asdf) { //0xcbfe
		this.mem.write(this.hl, (byte) (this.mem.read(hl) | (1 << 7)));
	}

	private void set7A(int[] asdf) { //0xcbff
		this.af |= (1 << 7) << 8;
	}

	private void resa(int bit) {
		this.af = ((res(bit, this.af >> 8) & 0xff) << 8) | (this.af & 0xff);
	}

	//Disables a bit in a register
	private int res(int bit, int num) {
		return num & ~(1 << bit);
	}

	//Bitwise xor with flag setting
	private void xor(int value) {
		int a = (this.af >> 8) & 0xff;
		a ^= value;
		if (a == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}
		this.af &= ~(FLAG_CARRY | FLAG_NEG | FLAG_HALF_CARRY);
		this.af = (this.af & 0xff) | ((a << 8) & 0xff00);
	}

	//Bitwise and with flag setting
	private void and(int value) {
		int a = (this.af >> 8) & 0xff;
		a &= value;
		if (a == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}
		this.af &= ~(FLAG_CARRY | FLAG_NEG | FLAG_HALF_CARRY);
		this.af |= FLAG_HALF_CARRY;
		this.af = (this.af & 0xff) | ((a << 8) & 0xff00);
	}

	//Bitwise or with flag setting
	private void or(int value) {
		int a = (this.af >> 8) & 0xff;
		a |= value;
		if (a == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}
		this.af &= ~(FLAG_CARRY | FLAG_NEG | FLAG_HALF_CARRY);
		this.af = (this.af & 0xff) | ((a << 8) & 0xff00);
	}

	//Decrement with flag setting
	private int dec(int i) {
		if ((i & 0xf) == 0) {
			this.af |= FLAG_HALF_CARRY;
		} else {
			this.af &= ~FLAG_HALF_CARRY;
		}
		if (((i - 1) & 0xffff) == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}

		this.af |= FLAG_NEG;
		return (i - 1) & 0xffff;
	}

	//Increment byte with flag setting
	private int incByte(int i) {
		if ((i & 0xf) == 0xf) {
			this.af |= FLAG_HALF_CARRY;
		} else {
			this.af &= ~FLAG_HALF_CARRY;
		}

		if (((i + 1) & 0xff) == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}

		this.af &= ~FLAG_NEG;

		return (i + 1) & 0xff;
	}

	//Compare 2 values, set flags
	private void cp(int a, int b) {

		this.af |= FLAG_NEG;
		if (a == b) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}

		if (b > a) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}

		if ((b & 0xf) > (a & 0xf)) {
			this.af |= FLAG_HALF_CARRY;
		} else {
			this.af &= ~FLAG_HALF_CARRY;
		}

	}

	//Swap nibbles, set flags
	private byte swap(byte b) {
		byte out = (byte) (((b & 0xf) << 4) | ((b & 0xf0) >> 4));
		if (out == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}

		this.af &= ~(FLAG_NEG | FLAG_CARRY | FLAG_HALF_CARRY);

		return out;
	}

	//Add bytes with carry
	private int adcBytes(byte x, byte y) {
		int carry = (((this.af & FLAG_CARRY) > 0) ? 1 : 0);
		int out = addBytes(x, (byte) (y + carry)) & 0xff;
		if ((x & 0xf) + (y & 0xf) + carry >= 0x10) {
			this.af |= FLAG_HALF_CARRY;
		}
		if ((x & 0xff) + (y & 0xff) + carry > 0xff) {
			this.af |= FLAG_CARRY;
		}
		return out & 0xff;
	}

	//Add 2 bytes
	private int addBytes(byte x, byte y) {
		byte out = (byte) (x + y);
		int bigSum = (x & 0xff) + (y & 0xff);
		if (bigSum > 0xff) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}
		if ((x & 0xf) + (y & 0xf) > 0xf) {
			this.af |= FLAG_HALF_CARRY;
		} else {
			this.af &= ~FLAG_HALF_CARRY;
		}
		if (out == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}
		this.af &= ~FLAG_NEG;
		return out & 0xff;
	}

	//Subtract with carry bytes
	private int sbcBytes(byte x, byte y) {
		int carry = 0;
		if ((this.af & FLAG_CARRY) > 0) {
			carry = 1;
		}
		int temp = ((x & 0xff) - (y & 0xff) - carry);
		if (temp < 0) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}
		if ((y & 0xf) + carry > (x & 0xf)) {
			this.af |= FLAG_HALF_CARRY;
		} else {
			this.af &= ~FLAG_HALF_CARRY;
		}
		if ((temp & 0xff) == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}
		this.af |= FLAG_NEG;
		return temp & 0xff;
	}

	//Subtract bytes
	private int subBytes(byte x, byte y) {
		byte out = (byte) (x - y);
		if ((y & 0xff) > (x & 0xff)) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}
		if ((y & 0xf) > (x & 0xf)) {
			this.af |= FLAG_HALF_CARRY;
		} else {
			this.af &= ~FLAG_HALF_CARRY;
		}
		if (out == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}
		this.af |= FLAG_NEG;
		return out & 0xff;
	}

	//Used for debugging, runs private functions
	public void privateHook() {
		int out = this.rrc((byte) 0xfe) & 0xff;
		System.out.printf("0x%02x\n", out);
	}

	//Adds 2 shorts, sets flags
	private int addShorts(short x, short y) {
		int out = (x + y) & 0xffff;
		int bigSum = (x & 0xffff) + (y & 0xffff);
		if (bigSum > 0xffff) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}
		if ((x & 0xfff) + (y & 0xfff) > 0xfff) {
			this.af |= FLAG_HALF_CARRY;
		} else {
			this.af &= ~FLAG_HALF_CARRY;
		}
		//this.af |= (FLAG_HALF_CARRY & ((x ^ y ^ out) >> 7));
		//System.out.println(this.af & FLAG_HALF_CARRY);

		/*	if (out == 0) {
				this.af |= FLAG_ZERO;
			} else {
				this.af &= ~FLAG_ZERO;
			}*/

		this.af &= ~FLAG_NEG;
		return out;
	}

	//Alternate add 2 shorts
	private int addShorts2(short x, short y) {
		int out = (x + (byte) y) & 0xffff;

		if ((x & 0xff) + (y & 0xff) > 0xff) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}
		if ((x & 0xf) + (y & 0xf) > 0xf) {
			this.af |= FLAG_HALF_CARRY;
		} else {
			this.af &= ~FLAG_HALF_CARRY;
		}

		this.af &= ~(FLAG_NEG | FLAG_ZERO);
		return out;
	}

	//Checks a bit, sets a flag
	private void bit(byte bit, byte val) {
		if ((val & bit) == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}

		this.af &= ~FLAG_NEG;
		this.af |= FLAG_HALF_CARRY;
	}

	//Shift right logical (010 > 001, 110 > 011)
	private int srl(byte b) {
		if ((b & 0x1) > 0) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}

		b = (byte) ((b & 0xff) >> 1);

		if (b == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}

		this.af &= ~(FLAG_NEG | FLAG_HALF_CARRY);

		return b & 0xff;
	}

	//Shift right arithmetic (010 > 001, 110 > 111)
	private int sra(byte b) {
		if ((b & 0x1) > 0) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}

		b = (byte) ((b & 0xff) >> 1);
		b |= (b & (1 << 6)) << 1;

		if (b == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}

		this.af &= ~(FLAG_NEG | FLAG_HALF_CARRY);

		return b & 0xff;
	}

	//Shift left arithmetic
	private int sla(byte b) {
		if ((b & 0x80) > 0) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}

		b <<= 1;

		if (b == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}

		this.af &= ~(FLAG_NEG | FLAG_HALF_CARRY);

		return b & 0xff;
	}

	//Rotate left
	private int rl(byte b) {
		int carry = (((this.af & FLAG_CARRY) != 0) ? 1 : 0);
		int f = (b & 0x80) >> 3;

		b = (byte) (((b << 1) & 0xff) | carry);

		if ((b & 0xff) == 0) {
			f |= FLAG_ZERO;
		}

		this.af = (this.af & 0xff00) | (f & 0xff);

		return b & 0xff;
	}

	//Rotate right
	private int rr(byte by) {
		int b = by & 0xff;
		boolean shouldCarry = (b & 0x1) > 0;
		b = (b & 0xff) >> 1;

		if ((this.af & FLAG_CARRY) > 0) {
			b |= 0x80;
		}

		if (shouldCarry) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}

		if (b == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}

		this.af &= ~(FLAG_NEG | FLAG_HALF_CARRY);

		return (b & 0xff);
	}

	//Rotate left with carry
	private int rlc(byte b) {
		int carry = (b >> 7) & 1;
		if (carry > 0) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}

		b = (byte) (((b & 0xff) << 1) & 0xff);
		b |= carry;
		//TODO wrong?
		this.af &= ~(FLAG_NEG | FLAG_HALF_CARRY);

		if (b == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}
		return b & 0xff;
	}

	//Rotate right with carry
	private int rrc(byte b) {
		int carry = b & 1;
		if (carry > 0) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}

		b = (byte) (((b & 0xff) >> 1) & 0xff);
		b |= carry << 7;
		//TODO wrong?
		this.af &= ~(FLAG_NEG | FLAG_HALF_CARRY);

		if (b == 0) {
			this.af |= FLAG_ZERO;
		} else {
			this.af &= ~FLAG_ZERO;
		}
		return b & 0xff;
	}

	//Alternate rotate right
	private int rrc2(byte b) {
		int carry = b & 1;

		if (carry > 0) {
			this.af |= FLAG_CARRY;
		} else {
			this.af &= ~FLAG_CARRY;
		}

		b = (byte) (((b & 0xff) >> 1) & 0xff);
		b |= carry << 7;
		//TODO wrong?
		this.af &= ~(FLAG_NEG | FLAG_HALF_CARRY | FLAG_ZERO);

		return b & 0xff;
	}

	//Instruction method is the interface that all method handles in here are made into
	public interface InstructionMethod {
		public void execute(int... bytes);
	}

	//Pushes an pops stuff to and from stack
	private void pushByte(byte b) {
		this.sp--;
		this.mem.write(this.sp, b);
	}

	private void pushShort(short s) {
		pushByte((byte) (s >> 8));
		pushByte((byte) s);
	}

	private int popByte() {
		return this.mem.read(this.sp++);
	}

	private int popShort() {
		int lower = popByte();
		int upper = popByte();
		int ret = (((upper & 0xff) << 8) | (lower & 0xff));
		return ret;
	}

	//Debugging stuff
	public boolean areIntsEnabled() {
		return this.intsEnabled;
	}

	public int getPC() {
		return this.pc;
	}

	//Basic instruction implementation, supports disassembly and execution
	public class Instruction {
		private InstructionMethod m;
		private String d;
		private int argCount;

		public final int baseTicks;

		public Instruction(String dissass, int argCount, InstructionMethod method, int ticks) {
			this.m = method;
			this.d = dissass;
			this.argCount = argCount;
			this.baseTicks = ticks;
		}

		public String dissassemble(int addr) {
			String out = "";
			if (this.d.indexOf("%hex%") > 0) {
				String[] parts = this.d.split(Pattern.quote("%hex%"));
				int val = readBytesAsInt(addr + 1, this.argCount);
				out = parts[0];
				out += String.format("%0" + (this.argCount * 2) + "x", val);
				if (parts.length > 1) {
					out += parts[1];
				}
			} else if (this.d.indexOf("%signed%") > 0) {
				String[] parts = this.d.split(Pattern.quote("%signed%"));
				int val = readBytesAsInt(addr + 1, this.argCount);
				out = parts[0];
				out += Integer.toString((byte) val);
				if (parts.length > 1) {
					out += parts[1];
				}
			} else {
				out = this.d;
			}
			return out;
		}

		public InstructionMethod getMethod(int addr) {
			return m;
		}

		public int getArgCount(int addr) {
			return argCount;
		}

		public boolean implemented(int addr) {
			return true;
		}
	}

	//Used while implementing all the opcode, kept in to illustrate development cycle
	class Unimplemented extends Instruction {
		public Unimplemented(int count) {
			super(String.format("Unimplemented instruction: 0x%02x", count), 0, new InstructionMethod() {
				@Override
				public void execute(int... bytes) {
				}
			}, 0);
		}

		@Override
		public boolean implemented(int addr) {
			return false;
		}
	}

	//Used to crash for nonexistent instructions - set by CPU spec
	class Nonexistant extends Instruction {
		public Nonexistant(int count) {
			super(String.format("Nonexistant instruction: 0x%02x", count), 0, new InstructionMethod() {
				@Override
				public void execute(int... bytes) {
				}
			}, 0);
		}

		@Override
		public boolean implemented(int addr) {
			return false;
		}
	}

	//Various memory reading utilities
	private int[] readBytesFromMemory(int len) {
		int[] out = readBytesFromMemory(this.pc, len);
		this.pc += len;
		return out;
	}

	private int[] readBytesFromMemory(int addr, int len) {
		int[] out = new int[len];
		for (int i = 0; i < len; i++) {
			out[len - i - 1] = mem.read(addr + i) & 0xff;
		}
		return out;
	}

	private int readBytesAsInt(int addr, int len) {
		int[] bytes = readBytesFromMemory(addr, len);
		int out = 0;
		for (int i : bytes) {
			out <<= 8;
			out |= i;
		}
		return out;
	}

	//Push the program counter, call interrupt handler
	public void gotoInterrupt(int addr) {
		this.intsEnabled = false;
		this.pushShort((short) this.pc);
		this.inInterrupt = true;
		this.pc = addr;
		this.c.inc(12);
	}

	public void exitHalt(){
		//this.haltFlag |= 1;
		this.haltFlag = false;
		//System.out.println("Disabled halt");
	}

	public boolean inInterrupt() {
		return this.inInterrupt;
	}

	//Sort of workaround to run the extended instructions without modifying my existing datastructure
	private class ExtendedInstructions extends Instruction {

		public ExtendedInstructions() {
			super(null, 0, null, 0);
		}

		@Override
		public String dissassemble(int addr) {
			if (extInstructions[mem.read(addr + 1) & 0xff] instanceof Unimplemented) {
				System.out.println("Unimplemented instruction: 0xcb" + String.format("%02x", mem.read(addr + 1)));
				//return "Unimplemented instruction: 0xcb" + String.format("%02x", mem.read(addr + 1));
			}
			return extInstructions[mem.read(addr + 1) & 0xff].dissassemble(addr + 1);
		}

		@Override
		public InstructionMethod getMethod(int addr) {
			return new MethodWrapper(extInstructions[mem.read(addr + 1) & 0xff], addr + 1);
		}

		@Override
		public int getArgCount(int addr) {
			return extInstructions[mem.read(addr + 1) & 0xff].getArgCount(addr + 1) + 1;
		}

		@Override
		public boolean implemented(int addr) {
			return extInstructions[mem.read(addr + 1) & 0xff].implemented(addr + 1);
		}

		private class MethodWrapper implements InstructionMethod {

			public final Instruction inst;
			int a;

			public MethodWrapper(Instruction i, int addr) {
				this.inst = i;
				this.a = addr;
			}

			@Override
			public void execute(int... bytes) {
				if (bytes == null) {
					this.inst.getMethod(this.a).execute(null);
				} else {
					int[] args = new int[bytes.length - 1];
					for (int i = 0; i < args.length; i++)
						args[i] = bytes[i + 1];
					this.inst.getMethod(this.a).execute(args);
				}
				c.inc(this.inst.baseTicks);
			}

		}
	}
}
