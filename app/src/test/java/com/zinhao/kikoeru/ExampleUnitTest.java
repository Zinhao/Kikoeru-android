package com.zinhao.kikoeru;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void vttTest(){
        Lrc lrc = new Lrc("WEBVTT\n" +
                "                                                                                                    \n" +
                "                                                                                                    00:00:02.725 --> 00:00:06.800\n" +
                "                                                                                                    客人 失礼啦\n" +
                "                                                                                                    \n" +
                "                                                                                                    00:00:10.025 --> 00:00:11.833\n" +
                "                                                                                                    欢迎光临\n" +
                "                                                                                                    \n" +
                "                                                                                                    00:00:12.075 --> 00:00:19.875\n" +
                "                                                                                                    非常感谢您今天也能光顾JK口交美容沙龙 唇之女王");

        for (int i = 0; i < lrc.getLrcRows().size(); i++) {
            System.out.println( lrc.getLrcRows().get(i).content);
        }
    }
}