package com.zinhao.kikoeru.db;

import androidx.room.*;

import java.util.List;
@Dao
public interface AudioLrcBindDao {
    @Query("SELECT * FROM audio_lrc_bind WHERE rjNumber = :rjNumber and audioPath = :audioPath")
    List<AudioLrcBind> getLrcBind(long rjNumber, String audioPath);

    @Query("SELECT * FROM audio_lrc_bind WHERE audioPath = :audioPath")
    List<AudioLrcBind> getLrcBind(String audioPath);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLrcBind(AudioLrcBind audioLrcBind);

    @Update
    void updateLrcBind(AudioLrcBind audioLrcBind);

    @Delete
    void deleteLrcBind(AudioLrcBind audioLrcBind);
}
