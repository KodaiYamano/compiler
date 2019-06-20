package enshud.s0.trial;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.output.TeeOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;

/**
 * !!! このコードは編集禁止 !!!
 * 
 * Trialの単体テストクラス．
 * ここに記述された全ての単体テストが正しく動作するようにLexer.run()メソッドを開発すること．
 * 
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TrialTest {
	/**
	 * 全テストのタイムアウト時間の定義．
	 * 各テストは10秒以内に実行が完了すること．
	 * （無限ループ等の回避のため）
	 */
	@Rule
	public final Timeout globalTimeout = Timeout.seconds(10);

	/**
	 * System.exit()が呼び出されてもテストを継続する．
	 */
	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();

	/**
	 * 標準出力の出力先．
	 */
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();

	/**
	 * 標準エラーの出力先．
	 */
	private final ByteArrayOutputStream err = new ByteArrayOutputStream();

	/**
	 * 各テストの実行前に実行されるメソッド．
	 * 標準出力と標準エラーの出力先を捕まえておく．
	 */
	@Before
	public void before() {
		// 標準出力を，outと標準出力の両方に吐き出す （outはテストからの読み込み用，標準出力は画面上での確認用）
		final TeeOutputStream outTree = new TeeOutputStream(System.out, out);
		System.setOut(new PrintStream(outTree));

		// 標準エラーも同様
		final TeeOutputStream errTree = new TeeOutputStream(System.err, err);
		System.setErr(new PrintStream(errTree));
	}

	/**
	 * 各テストの実行後に実行されるメソッド．
	 * 捕まえておいた標準出力と標準エラーを閉じる．
	 * @throws IOException
	 */
	@After
	public void after() throws IOException {
		out.close();
		err.close();
	}

	/**
	 * 単体テスト1 （正常系）．
	 * 入力ファイルがnormal01.pasの時に正しく5を出力するかを確認．
	 */
	@Test
	public void testNormal01() {
		new Trial().run("data/pas/normal01.pas");
		assertThat(out.toString().trim()).isEqualTo("5");
	}

	/**
	 * 単体テスト2 （正常系）．
	 * 入力ファイルがnormal02.pasの時に正しく7を出力するを確認．
	 */
	@Test
	public void testNormal02() {
		new Trial().run("data/pas/normal02.pas");
		assertThat(out.toString().trim()).isEqualTo("7");
	}

	/**
	 * 単体テスト3 （正常系）．
	 * 入力ファイルがnormal12.pasの時に正しく182を出力するかを確認．
	 */
	@Test
	public void testNormal03() {
		new Trial().run("data/pas/normal12.pas");
		assertThat(out.toString().trim()).isEqualTo("182");
	}

	/**
	 * 単体テスト4 （正常系）．
	 * 入力ファイルが空の時に正しく0を出力するかを確認．
	 */
	@Test
	public void testXInputFileEmpty() throws IOException {
		// 空の一時ファイルを生成
		final String tempFileName = "tmp/xxx.pas";
		final Path tempPath = Paths.get(tempFileName);
		Files.deleteIfExists(tempPath);
		Files.createFile(tempPath);

		// Trialの実行
		new Trial().run("tmp/xxx.pas");

		// 一時ファイルを削除しておく
		Files.delete(tempPath);

		// 結果は0のはず
		assertThat(out.toString().trim()).isEqualTo("0");
	}

	/**
	 * 単体テスト5 （異常系）．
	 * 入力ファイルが存在しない場合に標準エラーへ"File not found"を出力するかを確認．
	 */
	@Test
	public void testXInputFileNotFound() {
		new Trial().run("data/pas/xxxxxxxx.pas");
		assertThat(err.toString().trim()).isEqualTo("File not found");
	}
}
