package sudoku;

import java.util.Random;

// 123|456|789
// 456|789|123
// 789|123|456
// ---+---+---
// 231|564|897
// 564|897|231
// 897|231|564
// ---+---+---
// 312|645|978
// 645|978|312
// 978|312|645
/**
 * 数独生成/求解(无对象封装，仅引入java.util.Random包用于随机)
 * <p><b>生成算法</b>
 * <li>按1-9的顺序，按block(上面被线包起来的部分)从左到右从上到下，依次随机生成数字</li>
 * <li>当无法生成数字时，先回溯到上一个block，如果已达第0个block，则回溯到上一个数字</li>
 * </p>
 * <p><b>删除算法</b>
 * <li>纯随机删除指定个数字，所以可能求解结果不唯一，此时需自行重试，这里未实现重试</li>
 * </p>
 * <p><b>求解算法</b>
 * <li>从左到右从上到下依次求出每个位置所有可能的数值集合ps</li>
 * <li>依次按照ps中的数值尝试求解，当某一位置无解时，回溯到上一位置的下一个可能的值，如果没有则继续回溯到上一位置，至少可得出一组解(生成 算法生成的那组解)</li>
 * <li>依次找到ps某一位置中未使用过的数值(正数)，其他位置数值保持原样，再次求解，若有解则此数独不满足唯一解要求</li>
 * <li>如果ps中没有未使用的数值，或ps中所有包含正数位置的情况都尝试过，且均无解，则第一次生成的即为唯一解</li>
 * </p>
 * 
 * @author black
 */
public class Sudoku {
	static final Random Rand = new Random();
	static final int Magic = 9;
	static final int Total = Magic * Magic;
	static final int Min = 17; // 至少保留的数字个数，少于此数值一定没有唯一解

	static int[] answer(int[] src) { // 对数独src求解，并尝试求出2个解，如果未算出2个解则证明数独有唯一解
		// 计算删除数字的个数
		int removeCount = 0;
		boolean[] rms = new boolean[Total];
		for (int i = 0; i < Total; ++i) {
			if (src[i] == 0) {
				rms[i] = true;
				++removeCount;
			}
		}
		if (removeCount > Total - Min) {
			System.out.println("ERROR!remove too much,maybe no unique answer!");
			return null;
		}
		System.out.println("removeCount:" + removeCount);
		// 根据行/列/block不可有重复的规则，计算出每个位置可能的所有数值集合
		int[][] ps = new int[Total][]; // 每个位置可能的所有数值集合
		for (int idx = 0; idx < Total; ++idx) {
			if (!rms[idx])
				continue;
			int[] p = possibles(src, idx);
			if (p[0] == 0) {
				System.out.println("ERROR!idx:" + idx + " has no possible number!");
				return null;
			}
			ps[idx] = p;
		}
		// 按所有可能数值集合ps进行求解
		int[] src1 = copy(src);
		boolean hasAnswer = tryAnswer(src1, ps);
		if (!hasAnswer) { // 无法得出解，应该是生成有问题
			System.out.println("ERROR! no answer. maybe gen process has bug");
			return null;
		}
		System.out.println("after answer====");
		printPossibles(ps); // 打印一次求解后的结果
		// 下面尝试求其他解，思路为
		// 如果ps中所有数位负值，表示所有没有其他可能的解，上面的src1即为唯一解
		// 如果ps中有正数，则按照顺序，依次去掉对应位置的负数，保留正数(未使用过的)，重新求解，新的解至少在这个位置的数值与src1不同
		// 重复此过程，直到所有含有正数的位置都试过，如果都没有得出新的解，则src1为唯一解
		hasAnswer = false;
		int retryCount = 0;
		while (!hasAnswer) {
			int retryIdx = needRetry(ps, ++retryCount);
			if (retryIdx < 0) // 有唯一解
			{
				System.out.println("unique answer");
				return src1;
			}
			int[][] rp = prepareRetry(retryIdx, ps);
			int[] src2 = copy(src);
			hasAnswer = tryAnswer(src2, rp);
			if (hasAnswer) {
				System.out.println("WARN!has more than one answer");
				System.out.println("=====answer1");
				print(src1);
				checkPrint(src1);
				System.out.println("=====answer2");
				print(src2);
				checkPrint(src2);
				for (int i = 0; i < Magic * Magic; ++i) {
					if (src1[i] != src2[i]) {
						System.out.println("src1 is diff with src2 idx: " + i + " " + src1[i] + " " + src2[i]);
						return null;
					}
				}
				System.out.println("ERROR! src1 is the same as src2! it must be a bug!");
				printPossibles(rp);
				return null;
			}
		}
		return null;
	}

	static int needRetry(int[][] ps, int retryCount) { // 如果ps中有retryCount个包含正数的位置则表示还可能有解
		int count = 0; // 找到第retryCount个包含正数(未使用过)的ps
		for (int j = 0; j < Magic * Magic; ++j) {
			int[] p = ps[j];
			if (p == null)
				continue;
			for (int i = 0; i < Magic; ++i) {
				int n = p[i];
				if (n == 0)
					break;
				if (n > 0) {
					++count;
					break;
				}
			}
			if (count == retryCount)
				return j;
		}
		return -1;
	}

	static int[][] prepareRetry(int retryIdx, int[][] ps) { // 重新求解的准备工作
		int[][] ret = new int[Magic * Magic][]; // 找到第一个包含正数(未使用过)的ps，删掉其中的负数，同时将负数变回正数，用于重试
		for (int j = 0; j < Magic * Magic; ++j) {
			int[] p0 = ps[j];
			if (p0 == null)
				continue;
			ret[j] = copy(p0);
			int[] p = ret[j];
			if (retryIdx == j) { // 仅保留正数
				int[] tmp = new int[Magic];
				int tmpIdx = 0;
				for (int i = 0; i < Magic; ++i) {
					int n = p[i];
					if (n == 0)
						break;
					if (n > 0)
						tmp[tmpIdx++] = n;
				}
				for (int i = 0; i < Magic; ++i)
					p[i] = tmp[i];
				continue;
			}
			for (int i = 0; i < Magic; ++i) {
				int n = p[i];
				if (n >= 0)
					break;
				if (n < 0)
					p[i] = -n;
			}
		}
		return ret;
	}

	static boolean tryAnswer(int[] src, int[][] ps) { // 根据所有可能数字数组ps，计算src的一个解，无解时返回false，同时会将ps中用过的数字变为负数
		int firstIdx = -1;
		boolean back = false;
		int idx = 0;
		for (; idx < Magic * Magic;) {
			int[] p = ps[idx];
			if (p == null) {
				if (back)
					--idx;
				else
					++idx;
				continue;
			}
			if (firstIdx < 0)
				firstIdx = idx;
			if (back) // 回溯时清除之前填的数值
				src[idx] = 0;
			for (int i = 0; i < Magic; ++i) {
				int n = p[i];
				if (n == 0)
					break;
				if (n < 0) {
					continue;
				}
				p[i] = -n; // 负数表示已用过
				src[idx] = n;
				if (!checkLine(src, idx) || !checkCol(src, idx) || !checkBlock(src, idx))
					src[idx] = 0;
				else
					break;
			}
			if (src[idx] == 0) { // 回溯到上一个可能的数组
				for (int i = 0; i < Magic; ++i) // 回溯时清除已用过的记录
					p[i] = -p[i];
				--idx;
				if (idx < firstIdx)
					return false;
				back = true;
			} else {
				++idx;
				back = false;
			}
		}
		return true;
	}

	static int[] possibles(int[] src, int srcIdx) { // 计算srcIdx位置所有可能的数字(满足行/列/block不重复)
		int[] used = new int[Magic]; // 行/列/block已经使用过的数字
		int rowNum = srcIdx / Magic;
		int rowStartIdx = Magic * rowNum;
		int colStartIdx = srcIdx % 9;
		for (int i = 0; i < Magic; ++i) {
			// 同一行的数值
			int n = src[rowStartIdx + i];
			if (n != 0)
				used[n - 1] = n;
			// 同一列的数值
			n = src[colStartIdx + i * Magic];
			if (n != 0)
				used[n - 1] = n;
		}

		int blockStartRow = 3 * (rowNum / 3);
		int blockStartCol = 3 * (colStartIdx / 3);
		int blockStartIdx = blockStartRow * Magic + blockStartCol;
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 3; ++j) {
				int idx = blockStartIdx + i * Magic + j;
				int n = src[idx];
				if (n != 0)
					used[n - 1] = n;
			}
		}
		int[] ps = new int[Magic]; // 得出srcIdx位置处可能的数字
		int psIdx = 0;
		for (int i = 0; i < Magic; ++i) {
			int n = used[i];
			if (n == 0)
				ps[psIdx++] = i + 1;
		}
		return ps;
	}

	static void printPossibles(int[][] ps) { // 打印可能数组
		for (int j = 0; j < Magic * Magic; ++j) {
			int[] p = ps[j];
			if (p == null)
				continue;
			StringBuilder sb = new StringBuilder(Magic * 2 + 10);
			for (int i = 0; i < Magic; ++i) {
				if (p[i] != 0)
					sb.append(p[i]).append(",");
				else {
					sb.deleteCharAt(sb.length() - 1);
					break;
				}
			}
			System.out.println("idx:" + j + " [" + sb + "]");
		}
	}

	static int[] copy(int[] src) {
		int[] ret = new int[src.length];
		for (int i = 0, n = src.length; i < n; ++i)
			ret[i] = src[i];
		return ret;
	}

	static void remove(int[] src, int removeCount) { // 从src中随机删除removeCount个数字
		int[] rms = new int[removeCount];
		int rmsIdx = 0;
		int[] rmsTmp = new int[Total];
		for (int i = 0; i < Total; ++i)
			rmsTmp[i] = i;
		for (int i = 0; i < removeCount; ++i) {
			int k = Rand.nextInt(Total - rmsIdx);
			int idx = rmsTmp[k];
			rms[rmsIdx++] = idx;
			src[idx] = 0;
			for (int j = k + 1; j < Total; ++j) // 删除随机到的k(将k之后的数字都向前移动一个位置)
				rmsTmp[j - 1] = rmsTmp[j];
		}
	}

	static void gen(int[] arr, int[][][] records) { // 生成一个完全的数独(没有剔除任何数字的)
		int n = 1, blockIdx = 0;
		loop: for (; n <= Magic;) {
			for (; blockIdx < Magic;) {
				int arrIdx = randBlock(arr, records, n, blockIdx);
				if (arrIdx < 0) {
					--blockIdx;
					System.out.println("backtrace blockIdx: " + blockIdx);
					if (blockIdx < 0) {
						--n;
						blockIdx = 0;
						if (n == 0) {
							n = 1;
							System.out.println("ERROR! backtrace to start");
							return;
						}
						for (int i = 0; i < Magic; ++i) {
							for (int j = 0, m = records[n][i].length; j < m; ++j)
								records[n][i][j] = 0;
						}
						System.out.println("backtrace n: " + n);
						continue loop;
					}
					continue;
				}
				++blockIdx;
			}
			++n;
			blockIdx = 0;
		}
	}

	static int randBlock(int[] arr, int[][][] records, int n, int blockIdx) { // 将数字n随机到block中的一个位置
		// System.out.println("n:" + n + "," + blockIdx);
		int startRowIdx = 3 * (blockIdx / 3) * Magic;
		int colOffset = 3 * (blockIdx % 3);
		int blockStartIdx = startRowIdx + colOffset;

		int u = 10;
		int[] used = new int[Magic];
		for (int i = 0; i < 3; ++i) {
			int t = i * Magic;
			// 检测被占用的行
			int rowIdx = startRowIdx + t;
			for (int c = 0; c < colOffset; ++c) {
				int idx = rowIdx + c;
				if (arr[idx] == n) {
					int uIdx = i * 3;
					used[uIdx] = u;
					used[uIdx + 1] = u;
					used[uIdx + 2] = u;
					break;
				}
			}
			// 检测被占用的列
			for (int rIdx = blockStartIdx + i; rIdx >= 0; rIdx -= Magic) {
				if (arr[rIdx] == n) {
					used[i] = u;
					used[i + 3] = u;
					used[i + 6] = u;
					break;
				}
			}
			// 已存在的其他数字
			for (int j = 0; j < 3; ++j) {
				int idx = blockStartIdx + t + j;
				int v = arr[idx];
				if (v >= n)
					arr[idx] = 0; // 回溯时之前生成的数需去掉
				else if (v != 0)
					used[i * 3 + j] = u;

			}
		}
		// 之前随机到的位置
		int[] record = records[n - 1][blockIdx];
		for (int i = 0; i < Magic; ++i) {
			if (record[i] != 0)
				used[i] = u;
		}

		int randIdx = randUnused(used);
		if (randIdx < 0)
			return -1;
		record[randIdx] = n;
		int arrIdx = blockStartIdx + (randIdx / 3) * Magic + randIdx % 3;
		arr[arrIdx] = n;
		return arrIdx;
	}

	static int randUnused(int[] used) { // 通过已使用过的数值，在未使用的数值中随机一个
		int count = 0;
		int[] available = new int[Magic];
		for (int i = 0; i < Magic; ++i) {
			if (used[i] == 0)
				available[count++] = i;
		}
		if (count > 0)
			return available[Rand.nextInt(count)];
		return -1;
	}

	static int[] initDups() {
		return new int[Magic];
	}

	static boolean checkDups(int n, int[] dups) { // 检测行/列/block中是否有重复的数字
		if (n == 0)
			return true;
		int idx = n - 1;
		if (dups[idx] != 0)
			return false;
		dups[idx] = n;
		return true;
	}

	static boolean checkLine(int[] src, int arrIdx) { // 检测arrIdx所在行是否有重复的数字
		int lineNum = arrIdx / Magic;
		int startIdx = lineNum * Magic;
		int endIdx = startIdx + Magic;
		int[] dups = initDups();
		for (int i = startIdx; i < endIdx; ++i) {
			if (!checkDups(src[i], dups))
				return false;
		}
		return true;
	}

	static boolean checkCol(int[] src, int arrIdx) { // 检测arrIdx所在列是否有重复的数字
		int colNum = arrIdx % Magic;
		int[] dups = initDups();
		for (int i = 0; i < Magic; ++i) {
			int colIdx = i * Magic + colNum;
			if (!checkDups(src[colIdx], dups))
				return false;
		}
		return true;
	}

	static boolean checkBlock(int[] src, int arrIdx) { // 检测arrIdx所在block是否有重复的数字
		int lineNum = arrIdx / Magic;
		int colNum = arrIdx % Magic;
		int blockStartLine = 3 * (lineNum / 3);
		int blockStartCol = 3 * (colNum / 3);
		int blockStartIdx = blockStartLine * Magic + blockStartCol;
		int[] dups = initDups();
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 3; ++j) {
				int idx = blockStartIdx + i * Magic + j;
				if (!checkDups(src[idx], dups))
					return false;
			}
		}
		return true;
	}

	static void print(int[] src) { // 打印数独
		for (int i = 0; i < Magic; ++i) {
			System.out.print(i + ": ");
			for (int j = 0; j < Magic; ++j) {
				int n = src[i * Magic + j];
				System.out.print((n != 0 ? n : " ") + " " + ((j == 2 || j == 5) ? "|" : ""));
			}
			System.out.println((i == 2 || i == 5) ? "\n----------------------" : "");

		}
	}

	static void checkPrint(int[] src) { // 检测并打印数独
		for (int i = 0; i < Magic * Magic; ++i) {
			if (!checkLine(src, i) || !checkCol(src, i) || !checkBlock(src, i)) {
				System.out.println("check ERROR! idx:" + i + " break the rule!");
				return;
			}
		}
		System.out.println("ok");
	}

	public static void main(String[] args) throws Exception {
		int[] src = new int[Magic * Magic]; // 数独数组
		int[][][] records = new int[Magic][Magic][Magic]; // 生成时每个数曾随机到的位置记录，用于回溯

		Sudoku.gen(src, records);
		System.out.println("========gen");
		Sudoku.print(src);
		Sudoku.checkPrint(src);

		int removeCount = 40; // 可以控制难度，但难度越高，非唯一解的概率越高
		System.out.println("========remove " + removeCount);
		int[] removed = new int[src.length];
		for (int i = 0, n = src.length; i < n; ++i)
			removed[i] = src[i];
		Sudoku.remove(removed, removeCount);
		Sudoku.print(removed);

		System.out.println("answer=========");
		int[] answered = Sudoku.answer(removed);
		if (answered == null) {
			System.out.println("WARN!no unique answer");
			return;
		}
		Sudoku.print(answered);

		for (int i = 0, n = src.length; i < n; ++i) {
			if (src[i] != answered[i]) {
				System.out.println("ERROR! check unique answer is buggy");
				return;
			}
		}
	}
}
