package io.github.ludongrong.netftp.watche;

import java.io.File;

import io.github.ludongrong.netftp.util.LogHelper;

/**
 * 标签创建.
 *
 * @author <a href="mailto:736779458@qq.com">ludongrong</a>
 * @since 2020-11-27
 */
public class TagAction extends TagFilter {

    /**
     * 构造.
     *
     * @param store
     *            本地存储目录
     */
    public TagAction(String store) {
        super(store, true);
    }

    /**
     * @see io.github.ludongrong.netftp.watche.TagFilter#doFilter(io.github.ludongrong.netftp.watche.WatcheParam,
     *      io.github.ludongrong.netftp.watche.FilterChain)
     */
    public void doFilter(WatcheParam watcheParam, FilterChain filterChain) {

        File tagFile = getTagFile(watcheParam, filterChain);
        if (tagFile.exists() == false) {
            try {
                if (watcheParam.isDirectory() == false) {
                    File tagParent = tagFile.getParentFile();
                    if (tagParent.exists() == false) {
                        tagParent.mkdirs();
                    }

                    tagFile.createNewFile();
                } else {
                    tagFile.mkdirs();
                }
            } catch (Exception e) {
                LogHelper.getLog().error("[{}]", tagFile.getAbsolutePath(), e);
            }
        }

        filterChain.doFilter(watcheParam);
    }
}