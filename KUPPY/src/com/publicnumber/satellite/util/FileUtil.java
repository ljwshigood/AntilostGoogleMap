package com.publicnumber.satellite.util;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import com.publicnumber.satellite.bean.RecordInfo;

public class FileUtil {

	/*
	 * private static int itemh = 150;
	 * 
	 * private static int itemw = 150;
	 * 
	 * private static LinkedList<String> extens = null;
	 * 
	 * public static ArrayList<ImageFolder> mImageFolderList = new
	 * ArrayList<ImageFolder>();
	 * 
	 * public static void getFiles(String path, IUIInterface ui) { File f = new
	 * File(path); File[] files = f.listFiles(); ImageFolder imagefolderInfo =
	 * new ImageFolder(); imagefolderInfo.path = path; if (files != null) { for
	 * (int i = 0; i < files.length; i++) { final File ff = files[i]; if
	 * (ff.isDirectory()) { getFiles(ff.getPath(), ui); } else { String fName =
	 * ff.getName(); if (fName.indexOf(".") > -1) { String end =
	 * fName.substring( fName.lastIndexOf(".") + 1, fName.length())
	 * .toUpperCase(); if (getExtens().contains(end)) {
	 * imagefolderInfo.filePathes.add(ff.getPath()); } } } } }
	 * 
	 * if (!imagefolderInfo.filePathes.isEmpty()) { imagefolderInfo.pisNum =
	 * imagefolderInfo.filePathes.size(); mImageFolderList.add(imagefolderInfo);
	 * ui.updateUI(mImageFolderList); } }
	 * 
	 * public static ArrayList<String> getRecordFiles(String path) { File f =
	 * new File(path); File[] files = f.listFiles();
	 * 
	 * ArrayList<String> filesList = new ArrayList<String>(); if (files != null)
	 * { for (int i = 0; i < files.length; i++) { final File ff = files[i]; if
	 * (ff.isDirectory()) { getRecordFiles(ff.getPath()); } else {
	 * if(getExtensionName(ff.getPath()).equals("amr")){
	 * filesList.add(ff.getPath()); }; } } } return filesList; }
	 * 
	 * public static String getExtensionName(String filename) { if ((filename !=
	 * null) && (filename.length() > 0)) { int dot = filename.lastIndexOf('.');
	 * if ((dot > -1) && (dot < (filename.length() - 1))) { return
	 * filename.substring(dot + 1); } } return filename; }
	 * 
	 * public static LinkedList<String> getExtens() { if (extens == null) {
	 * extens = new LinkedList<String>(); extens.add("JPEG"); extens.add("JPG");
	 * extens.add("PNG"); extens.add("GIF"); extens.add("BMP"); } return extens;
	 * }
	 * 
	 * public static Bitmap getDrawable(int index, int zoom, ArrayList<String>
	 * imagePathes) { if (index >= 0 && index < imagePathes.size()) { String
	 * path = imagePathes.get(index);
	 * 
	 * BitmapFactory.Options options = new BitmapFactory.Options();
	 * options.inJustDecodeBounds = true; BitmapFactory.decodeFile(path,
	 * options); int mWidth = options.outWidth; int mHeight = options.outHeight;
	 * int s = 1; while ((mWidth / s > itemw * 2 * zoom) || (mHeight / s > itemh
	 * * 2 * zoom)) { s *= 2; }
	 * 
	 * options = new BitmapFactory.Options(); options.inSampleSize = s;
	 * options.inPreferredConfig = Config.ARGB_8888; Bitmap bm =
	 * BitmapFactory.decodeFile(path, options);
	 * 
	 * if (bm != null) { int h = bm.getHeight(); int w = bm.getWidth();
	 * 
	 * float ft = (float) ((float) w / (float) h); float fs = (float) ((float)
	 * itemw / (float) itemh);
	 * 
	 * int neww = ft >= fs ? itemw * zoom : (int) (itemh * zoom * ft); int newh
	 * = ft >= fs ? (int) (itemw * zoom / ft) : itemh * zoom;
	 * 
	 * float scaleWidth = ((float) neww) / w; float scaleHeight = ((float) newh)
	 * / h;
	 * 
	 * Matrix matrix = new Matrix(); matrix.postScale(scaleWidth, scaleHeight);
	 * bm = Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true); return bm; } }
	 * return null; }
	 * 
	 * public static void deleteFile(File file) { if (file != null &&
	 * file.exists()) { file.delete(); }
	 * 
	 * }
	 */
	public static ArrayList<String> getFiles(String dirname) throws Exception {
		
		ArrayList<String> filesList = new ArrayList<String>();
		
		File dir = new File(dirname);
		if (dir.exists()) {
			File[] files = dir.listFiles();
			Arrays.sort(files, new CompratorByLastModified());
			for (int i = 0; i < files.length; i++) {
				filesList.add(files[i].getPath());
			}
		}
		return filesList;
	}

	public static ArrayList<String> getVideoFiles(String path) {
		File f = new File(path);
		File[] files = f.listFiles();

		ArrayList<String> filesList = new ArrayList<String>();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				final File ff = files[i];
				if (ff.isDirectory()) {
					getVideoFiles(ff.getPath());
				} else {
					if (getExtensionName(ff.getPath()).equals("jpg")) {
						filesList.add(ff.getPath());
					}
				}
			}
		}
		return filesList;
	}
		
	public static ArrayList<RecordInfo> getRecordFiles(String path) {
		File f = new File(path);
		File[] files = f.listFiles();

		ArrayList<RecordInfo> filesList = new ArrayList<RecordInfo>();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				final File ff = files[i];
				if (ff.isDirectory()) {
					getRecordFiles(ff.getPath());
				} else {
					if (getExtensionName(ff.getPath()).equals("amr")) {
						filesList.add(new RecordInfo(false,false,ff.getPath()));
					}
				}
			}
		}
		return filesList;
	}

	public static String getExtensionName(String filename) {
		if ((filename != null) && (filename.length() > 0)) {
			int dot = filename.lastIndexOf('.');
			if ((dot > -1) && (dot < (filename.length() - 1))) {
				return filename.substring(dot + 1);
			}
		}
		return filename;
	}

	public static void deleteFile(File file) {
		if (file != null && file.exists()) {
			file.delete();
		}
	}
}
