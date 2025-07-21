# PlaylistFragment 功能实现总结

## 实现的功能

### 1. 右上角排序功能
- **位置**: 右上角的排序按钮 (`button_sort`)
- **功能**: 点击后弹出底部菜单对话框，提供三种排序方式
- **排序选项**:
  - 名称 (name)
  - 歌曲数量 (songCount) 
  - 创建时间 (createTime)
- **实现方式**: 使用 `bottomMenusDialog` 显示排序选项
- **排序逻辑**: 
  - 名称排序: 按歌单名称字母顺序
  - 歌曲数量排序: 按歌单中的歌曲数量排序
  - 创建时间排序: 按歌单创建时间排序
- **UI更新**: 排序按钮文本会根据当前选择的排序方式更新

### 2. 左上角随机播放功能
- **位置**: 左上角的随机播放按钮 (`button_random`)
- **功能**: 点击后从所有歌单中随机选择歌曲进行播放
- **实现方式**: 
  - 获取所有非系统歌单
  - 从每个歌单中获取所有歌曲
  - 将所有歌曲合并后随机打乱顺序
  - 调用 `MainActivity.playFromPlaylist()` 方法播放歌曲
- **错误处理**: 当没有可播放歌曲时显示提示信息

## 技术实现细节

### 排序功能
```java
// 排序相关成员变量
private String currentSortType = "name"; // name, songCount, createTime
private String currentSortOrder = "asc"; // asc, desc

// 排序对话框
private void showSortDialog() {
    ArrayList<bottomDialogMenu> sortOptions = new ArrayList<>();
    sortOptions.add(new bottomDialogMenu(getString(R.string.sort_by_name), currentSortType.equals("name")));
    sortOptions.add(new bottomDialogMenu(getString(R.string.sort_by_song_count), currentSortType.equals("songCount")));
    sortOptions.add(new bottomDialogMenu(getString(R.string.sort_by_create_time), currentSortType.equals("createTime")));
    
    bottomMenusDialog sortDialog = new bottomMenusDialog(
        getContext(), sortOptions, getCurrentSortDisplayName(), 
        getString(R.string.sort_by), listener);
    sortDialog.show();
}
```

### 随机播放功能
```java
private void startRandomPlay() {
    executorService.execute(() -> {
        try {
            // 获取所有非系统歌单
            List<Playlist> playlists = LitePal.where("isSystemPlaylist = ?", "0")
                    .find(Playlist.class);
            
            List<Song> allPlaylistSongs = new ArrayList<>();
            
            // 从所有歌单中获取歌曲
            for (Playlist playlist : playlists) {
                List<Song> playlistSongs = PlaylistSong.getPlaylistSongs(playlist.getId());
                allPlaylistSongs.addAll(playlistSongs);
            }
            
            // 随机打乱歌曲列表
            Collections.shuffle(allPlaylistSongs);

            if (mainHandler != null) {
                mainHandler.post(() -> {
                    if (allPlaylistSongs != null && !allPlaylistSongs.isEmpty()) {
                        if (getActivity() instanceof MainActivity mainActivity) {
                            mainActivity.playFromPlaylist(allPlaylistSongs, 0);
                        }
                    } else {
                        ToastUtils.showToast(getContext(), getString(R.string.no_songs_to_play));
                    }
                });
            }
        } catch (Exception e) {
            // 错误处理
        }
    });
}
```

## 新增的字符串资源

在 `strings.xml` 中添加了以下字符串资源:
- `sort_by`: "排序方式"
- `sort_by_name`: "名称" 
- `sort_by_song_count`: "歌曲数量"
- `sort_by_create_time`: "创建时间"
- `no_songs_to_play`: "没有可播放的歌曲"
- `random_play_failed`: "随机播放失败"

## 文件修改

### 主要修改文件
1. `app/src/main/java/com/magicalstory/music/homepage/PlaylistFragment.java`
   - 添加排序相关成员变量和方法
   - 添加随机播放功能
   - 修改 `loadPlaylistData()` 方法支持不同排序方式
   - 添加排序按钮和随机播放按钮的初始化

2. `app/src/main/res/values/strings.xml`
   - 添加排序和随机播放相关的字符串资源

### 布局文件
- `app/src/main/res/layout/fragment_playlist.xml` 中已包含所需的按钮ID

## 使用方式

1. **排序功能**: 点击右上角的排序按钮，选择排序方式
2. **随机播放**: 点击左上角的随机播放按钮，开始随机播放歌曲

## 注意事项

- 排序功能会在选择后立即重新加载歌单数据
- 随机播放功能需要确保数据库中有可播放的歌曲
- 所有字符串都使用了资源文件，支持国际化
- 使用了后台线程处理数据库操作，避免阻塞UI线程 