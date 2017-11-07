package com.bow.lab.storage;

import edu.caltech.nanodb.storage.DBPage;

/**
 * @author vv
 * @since 2017/11/7.
 */
public interface PageWrapper {

    void initNewPage(DBPage dbPage);
}
