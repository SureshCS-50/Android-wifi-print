package com.sureshkumar.PrintDemo.services;

import android.app.Activity;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.*;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;
import com.sureshkumar.PrintDemo.Constants;
import com.sureshkumar.PrintDemo.Util;

import java.io.*;
import java.util.List;

/**
 * Created by Sureshkumar on 09-06-2015.
 */
public class PrintServicesAdapter extends PrintDocumentAdapter {
    private Activity mActivity;
    private int pageHeight;
    private int pageWidth;
    private PdfDocument myPdfDocument;
    private int totalpages = 1;
    private File pdfFile;
    private PrintCompleteService mPrintCompleteService;

    public PrintServicesAdapter(Activity mActivity, File pdfFile) {
        this.mActivity = mActivity;
        this.pdfFile = pdfFile;
        this.totalpages = Util.computePDFPageCount(pdfFile);
        this.mPrintCompleteService = (PrintCompleteService) mActivity;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback,
                         Bundle metadata) {
        myPdfDocument = new PrintedPdfDocument(mActivity, newAttributes);

        pageHeight =
                newAttributes.getMediaSize().getHeightMils() / 1000 * 72;
        pageWidth =
                newAttributes.getMediaSize().getWidthMils() / 1000 * 72;

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        if (totalpages > 0) {
            PrintDocumentInfo.Builder builder = new PrintDocumentInfo
                    .Builder(pdfFile.getName())
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(totalpages);

            PrintDocumentInfo info = builder.build();
            callback.onLayoutFinished(info, true);
        } else {
            callback.onLayoutFailed("Page count is zero.");
        }
    }

    @Override
    public void onWrite(final PageRange[] pageRanges,
                        final ParcelFileDescriptor destination,
                        final CancellationSignal cancellationSignal,
                        final WriteResultCallback callback) {
        InputStream input = null;
        OutputStream output = null;


        try {
            input = new FileInputStream(pdfFile);
            output = new FileOutputStream(destination.getFileDescriptor());
            byte[] buf = new byte[1024];
            int bytesRead;

            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }

            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});


        } catch (FileNotFoundException ee) {
            //Catch exception
        } catch (Exception e) {
            //Catch exception
        } finally {
            try {
                input.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                mPrintCompleteService.onMessage(Constants.PRINTER_STATUS_CANCELLED);
            }
        });
    }

    @Override
    public void onFinish() {
        mPrintCompleteService.onMessage(Constants.PRINTER_STATUS_COMPLETED);
    }
}