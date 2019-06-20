package enshud.s0.trial;
import java.io.*;



public class Trial {

	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Trial().run("data/pas/normal01.pas");
		new Trial().run("data/pas/normal02.pas");
		new Trial().run("data/pas/normal03.pas");
	}

	/**
	 * TODO
	 * 
	 * 開発対象となるTrial実行メソッド （練習用）．
	 * 以下の仕様を満たすこと．
	 * 
	 * 仕様:
	 * 第一引数で指定されたpascalファイルを読み込み，ファイル行数を標準出力に書き出す．
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 * 
	 * @param inputFileName 入力pascalファイル名
	 */
	public void run(final String inputFileName) {
		// カウントした行数を格納する整数型の変数を定義し、0で初期化する。
        long lineCount = 0;

		// 例外処理の始まり
        try
        {
			// ファイルを読み込みモードでオープンする。ファイルが存在しなかったりする場合に FileNotFoundException がスローされる。
            FileReader fr = new FileReader( inputFileName );

			// ファイルを読むための便利なクラス BufferedReader のオブジェクトを作る。
            BufferedReader br = new BufferedReader(fr);

			// 読み込んだ1行の文字列を格納するための変数を定義する。
            String line;

			// 1行目を読んでみる。もし、空のファイルなら、line には null がセットされる。
			line = br.readLine();
			
			// ファイルの最後まで来て null が返ってくるまで、処理を繰り返す。
            while( line != null )
            {
				// 1行読み込むに成功するたびに、行数のカウントを1増やす。
                lineCount++;
                
				// readLine メソッドを使ってもう1行読み込む。
                line = br.readLine();
            }
            
			// ストリームを閉じて、BufferedReader のリソースを開放する。
			// このとき、同時にFileReader のcloseも行われるので、fr.close() は必要ない。なので、ファイルもここで閉じられます。
            br.close();
        }
		catch( FileNotFoundException e )
		{
			// 15行目でエラーが発生するとここに来る。
            System.err.println("File not found");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// カウントした行数を返す。
        System.out.println(lineCount);
	}
}
