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
 * ��������/���(�޶����װ��������java.util.Random���������)
 * <p><b>�����㷨</b>
 * <li>��1-9��˳�򣬰�block(���汻�߰������Ĳ���)�����Ҵ��ϵ��£����������������</li>
 * <li>���޷���������ʱ���Ȼ��ݵ���һ��block������Ѵ��0��block������ݵ���һ������</li>
 * </p>
 * <p><b>ɾ���㷨</b>
 * <li>�����ɾ��ָ�������֣����Կ����������Ψһ����ʱ���������ԣ�����δʵ������</li>
 * </p>
 * <p><b>����㷨</b>
 * <li>�����Ҵ��ϵ����������ÿ��λ�����п��ܵ���ֵ����ps</li>
 * <li>���ΰ���ps�е���ֵ������⣬��ĳһλ���޽�ʱ�����ݵ���һλ�õ���һ�����ܵ�ֵ�����û����������ݵ���һλ�ã����ٿɵó�һ���(���� �㷨���ɵ������)</li>
 * <li>�����ҵ�psĳһλ����δʹ�ù�����ֵ(����)������λ����ֵ����ԭ�����ٴ���⣬���н��������������Ψһ��Ҫ��</li>
 * <li>���ps��û��δʹ�õ���ֵ����ps�����а�������λ�õ���������Թ����Ҿ��޽⣬���һ�����ɵļ�ΪΨһ��</li>
 * </p>
 * 
 * @author black
 */
public class Sudoku {
	static final Random Rand = new Random();
	static final int Magic = 9;
	static final int Total = Magic * Magic;
	static final int Min = 17; // ���ٱ��������ָ��������ڴ���ֵһ��û��Ψһ��

	static int[] answer(int[] src) { // ������src��⣬���������2���⣬���δ���2������֤��������Ψһ��
		// ����ɾ�����ֵĸ���
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
		// ������/��/block�������ظ��Ĺ��򣬼����ÿ��λ�ÿ��ܵ�������ֵ����
		int[][] ps = new int[Total][]; // ÿ��λ�ÿ��ܵ�������ֵ����
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
		// �����п�����ֵ����ps�������
		int[] src1 = copy(src);
		boolean hasAnswer = tryAnswer(src1, ps);
		if (!hasAnswer) { // �޷��ó��⣬Ӧ��������������
			System.out.println("ERROR! no answer. maybe gen process has bug");
			return null;
		}
		System.out.println("after answer====");
		printPossibles(ps); // ��ӡһ������Ľ��
		// ���波���������⣬˼·Ϊ
		// ���ps��������λ��ֵ����ʾ����û���������ܵĽ⣬�����src1��ΪΨһ��
		// ���ps��������������˳������ȥ����Ӧλ�õĸ�������������(δʹ�ù���)��������⣬�µĽ����������λ�õ���ֵ��src1��ͬ
		// �ظ��˹��̣�ֱ�����к���������λ�ö��Թ��������û�еó��µĽ⣬��src1ΪΨһ��
		hasAnswer = false;
		int retryCount = 0;
		while (!hasAnswer) {
			int retryIdx = needRetry(ps, ++retryCount);
			if (retryIdx < 0) // ��Ψһ��
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

	static int needRetry(int[][] ps, int retryCount) { // ���ps����retryCount������������λ�����ʾ�������н�
		int count = 0; // �ҵ���retryCount����������(δʹ�ù�)��ps
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

	static int[][] prepareRetry(int retryIdx, int[][] ps) { // ��������׼������
		int[][] ret = new int[Magic * Magic][]; // �ҵ���һ����������(δʹ�ù�)��ps��ɾ�����еĸ�����ͬʱ�����������������������
		for (int j = 0; j < Magic * Magic; ++j) {
			int[] p0 = ps[j];
			if (p0 == null)
				continue;
			ret[j] = copy(p0);
			int[] p = ret[j];
			if (retryIdx == j) { // ����������
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

	static boolean tryAnswer(int[] src, int[][] ps) { // �������п�����������ps������src��һ���⣬�޽�ʱ����false��ͬʱ�Ὣps���ù������ֱ�Ϊ����
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
			if (back) // ����ʱ���֮ǰ�����ֵ
				src[idx] = 0;
			for (int i = 0; i < Magic; ++i) {
				int n = p[i];
				if (n == 0)
					break;
				if (n < 0) {
					continue;
				}
				p[i] = -n; // ������ʾ���ù�
				src[idx] = n;
				if (!checkLine(src, idx) || !checkCol(src, idx) || !checkBlock(src, idx))
					src[idx] = 0;
				else
					break;
			}
			if (src[idx] == 0) { // ���ݵ���һ�����ܵ�����
				for (int i = 0; i < Magic; ++i) // ����ʱ������ù��ļ�¼
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

	static int[] possibles(int[] src, int srcIdx) { // ����srcIdxλ�����п��ܵ�����(������/��/block���ظ�)
		int[] used = new int[Magic]; // ��/��/block�Ѿ�ʹ�ù�������
		int rowNum = srcIdx / Magic;
		int rowStartIdx = Magic * rowNum;
		int colStartIdx = srcIdx % 9;
		for (int i = 0; i < Magic; ++i) {
			// ͬһ�е���ֵ
			int n = src[rowStartIdx + i];
			if (n != 0)
				used[n - 1] = n;
			// ͬһ�е���ֵ
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
		int[] ps = new int[Magic]; // �ó�srcIdxλ�ô����ܵ�����
		int psIdx = 0;
		for (int i = 0; i < Magic; ++i) {
			int n = used[i];
			if (n == 0)
				ps[psIdx++] = i + 1;
		}
		return ps;
	}

	static void printPossibles(int[][] ps) { // ��ӡ��������
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

	static void remove(int[] src, int removeCount) { // ��src�����ɾ��removeCount������
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
			for (int j = k + 1; j < Total; ++j) // ɾ���������k(��k֮������ֶ���ǰ�ƶ�һ��λ��)
				rmsTmp[j - 1] = rmsTmp[j];
		}
	}

	static void gen(int[] arr, int[][][] records) { // ����һ����ȫ������(û���޳��κ����ֵ�)
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

	static int randBlock(int[] arr, int[][][] records, int n, int blockIdx) { // ������n�����block�е�һ��λ��
		// System.out.println("n:" + n + "," + blockIdx);
		int startRowIdx = 3 * (blockIdx / 3) * Magic;
		int colOffset = 3 * (blockIdx % 3);
		int blockStartIdx = startRowIdx + colOffset;

		int u = 10;
		int[] used = new int[Magic];
		for (int i = 0; i < 3; ++i) {
			int t = i * Magic;
			// ��ⱻռ�õ���
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
			// ��ⱻռ�õ���
			for (int rIdx = blockStartIdx + i; rIdx >= 0; rIdx -= Magic) {
				if (arr[rIdx] == n) {
					used[i] = u;
					used[i + 3] = u;
					used[i + 6] = u;
					break;
				}
			}
			// �Ѵ��ڵ���������
			for (int j = 0; j < 3; ++j) {
				int idx = blockStartIdx + t + j;
				int v = arr[idx];
				if (v >= n)
					arr[idx] = 0; // ����ʱ֮ǰ���ɵ�����ȥ��
				else if (v != 0)
					used[i * 3 + j] = u;

			}
		}
		// ֮ǰ�������λ��
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

	static int randUnused(int[] used) { // ͨ����ʹ�ù�����ֵ����δʹ�õ���ֵ�����һ��
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

	static boolean checkDups(int n, int[] dups) { // �����/��/block���Ƿ����ظ�������
		if (n == 0)
			return true;
		int idx = n - 1;
		if (dups[idx] != 0)
			return false;
		dups[idx] = n;
		return true;
	}

	static boolean checkLine(int[] src, int arrIdx) { // ���arrIdx�������Ƿ����ظ�������
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

	static boolean checkCol(int[] src, int arrIdx) { // ���arrIdx�������Ƿ����ظ�������
		int colNum = arrIdx % Magic;
		int[] dups = initDups();
		for (int i = 0; i < Magic; ++i) {
			int colIdx = i * Magic + colNum;
			if (!checkDups(src[colIdx], dups))
				return false;
		}
		return true;
	}

	static boolean checkBlock(int[] src, int arrIdx) { // ���arrIdx����block�Ƿ����ظ�������
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

	static void print(int[] src) { // ��ӡ����
		for (int i = 0; i < Magic; ++i) {
			System.out.print(i + ": ");
			for (int j = 0; j < Magic; ++j) {
				int n = src[i * Magic + j];
				System.out.print((n != 0 ? n : " ") + " " + ((j == 2 || j == 5) ? "|" : ""));
			}
			System.out.println((i == 2 || i == 5) ? "\n----------------------" : "");

		}
	}

	static void checkPrint(int[] src) { // ��Ⲣ��ӡ����
		for (int i = 0; i < Magic * Magic; ++i) {
			if (!checkLine(src, i) || !checkCol(src, i) || !checkBlock(src, i)) {
				System.out.println("check ERROR! idx:" + i + " break the rule!");
				return;
			}
		}
		System.out.println("ok");
	}

	public static void main(String[] args) throws Exception {
		int[] src = new int[Magic * Magic]; // ��������
		int[][][] records = new int[Magic][Magic][Magic]; // ����ʱÿ�������������λ�ü�¼�����ڻ���

		Sudoku.gen(src, records);
		System.out.println("========gen");
		Sudoku.print(src);
		Sudoku.checkPrint(src);

		int removeCount = 40; // ���Կ����Ѷȣ����Ѷ�Խ�ߣ���Ψһ��ĸ���Խ��
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
