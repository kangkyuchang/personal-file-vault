package com.kkc.cloud.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.kkc.cloud.CloudConfig;
import com.kkc.cloud.data.LoginData;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class FileController {
	
	@Autowired
	private CloudConfig cloudConfig;

	@PostMapping("/upload")
	public ResponseEntity<Void> uploadFiles(@RequestParam("path") String path, @RequestParam("files") MultipartFile[] files, HttpSession session) throws IOException {
		String id = (String) session.getAttribute("id");
		if(id == null)
			return ResponseEntity.internalServerError().build();
		if(!id.equals(LoginData.adminId))
			return ResponseEntity.internalServerError().build();
		String uploadPath = cloudConfig.getStorePath();
		if(path != "")
			uploadPath += "/" + path;
		File dir = new File(uploadPath);
		if(!dir.exists())
			dir.mkdirs();
		for(MultipartFile file : files) {
			if(!file.isEmpty()) {
				File destination = new File(uploadPath, file.getOriginalFilename());
				file.transferTo(destination);
			}
		}
		return ResponseEntity.ok().build();
	}
	
	@GetMapping("/load")
	@ResponseBody
	public List<Map<String, Object>> getList(@RequestParam("directory") String directory, HttpSession session) throws Exception {
//		String id = (String) session.getAttribute("id");
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
//		if(id == null)
//			return list;
//		if(!id.equals(LoginData.adminId))
//			return list;
		if(directory == null)
			return list;
		String path = cloudConfig.getStorePath();
		if(!directory.equals("")) {
			path += "/" + directory;
		}
		System.out.println(path);
		File dir = new File(path);
		if(!dir.exists())
			return list;
		File[] files = dir.listFiles();
		if (files != null) {
			List<Map<String, Object>> filelist = new ArrayList<Map<String, Object>>();
		    for (File file : files) {
		    	Map<String, Object> map = new HashMap<String, Object>();
		    	map.put("name", file.getName());
		    	if(file.isFile()) {
		    		map.put("type", "file");
		    		filelist.add(map);
		    	}
		    	else {
		    		map.put("type", "directory");
		    		list.add(map);
		    	}
		    }
		    list.addAll(filelist);
		}
		return list;
	}
	
	@PostMapping("/remove")
	public ResponseEntity<Void> removeFiles(@RequestParam("fileName") List<String> fileNames, @RequestParam("path") String path, HttpSession session) {
		String id = (String) session.getAttribute("id");
		if(id == null)
			return ResponseEntity.badRequest().build();
		if(!id.equals(LoginData.adminId))
			return ResponseEntity.badRequest().build();
		for(String fileName : fileNames) {
			Path filePath = Path.of(cloudConfig.getStorePath(), path, fileName);
			File file = filePath.toFile();
			if(!file.exists()) {
				continue;
			}
			removeFile(file);
		}
		return ResponseEntity.ok().build();
	}
	
	private void removeFile(File file) {
		if(file.isDirectory()) {
			File[] children = file.listFiles();
			if(children != null) {
				for(File f : children) {
					removeFile(f);
				}
			}
		}
		file.delete();
	}
	
//	@GetMapping("/download")
//	public ResponseEntity<Resource> donwloadFile(@RequestParam("fileName") String fileName, @RequestParam("path") String location) throws Exception {
//		try {
//			String root = cloudConfig.getStorePath();
//			if(location != "") {
//				root += "\\" + location;
//			}
//			Path path = Paths.get(root).resolve(fileName).normalize();
//			Resource resource = new UrlResource(path.toUri());
//			System.out.println(path);
//			if(!resource.exists()) 
//				return ResponseEntity.notFound().build();
//			
//			String encodedFileName = URLEncoder.encode(resource.getFilename(), "UTF-8").replaceAll("\\+", "%20");
//			return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"").body(resource);
//		}
//		catch(MalformedURLException e) {
//			return ResponseEntity.badRequest().build();
//		}
//		
//	}
	
	@GetMapping("/download")
	public void donwloadFiles(@RequestParam("fileName") List<String> fileNames, @RequestParam("path") String path, HttpServletResponse response, HttpSession session) throws Exception {
		String id = (String) session.getAttribute("id");
		if(id == null) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "해당 파일에 접근할 권한이 없습니다.");
			return;
		}
		if(!id.equals(LoginData.adminId)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "해당 파일에 접근할 권한이 없습니다.");
			return;
		}
		if(fileNames.size() == 1) {
			Path filePath = Path.of(cloudConfig.getStorePath(), path, fileNames.get(0));
			File file = filePath.toFile();
			if(!file.exists()) {
				return;
			}
			if(!file.isDirectory()) {
				Resource resource = new UrlResource(filePath.toUri());
				response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
				
				if(!resource.exists())
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				
				String encodedFileName = URLEncoder.encode(resource.getFilename(), "UTF-8").replaceAll("\\+", "%20");
				response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
				InputStream in = resource.getInputStream();
				OutputStream out = response.getOutputStream();
				byte[] buffer = new byte[102400];
				int bytesRead;
				while((bytesRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
				out.flush();
				return;
			}
		}
		response.setContentType("application/zip");
		String encodedFileName = URLEncoder.encode("download.zip", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
		ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
		for(String fileName : fileNames) {
			Path filePath = Path.of(cloudConfig.getStorePath(), path, fileName);
			File file = filePath.toFile();
			if(!file.exists())
				continue;
			if(file.isDirectory()) {
				zipDirectory(file, fileName, zos);
			}
			else 
				zip(file, "", zos);
		}
		zos.finish();
	}
	
	private void zip(File file, String path, ZipOutputStream zos) throws Exception {
		String zipEntyName = path == "" ? file.getName() : path + "/" + file.getName();
		zos.putNextEntry(new ZipEntry(zipEntyName));
		FileInputStream fis = new FileInputStream(file);
		byte[] buffer = new byte[1024];
		int len;
		while((len = fis.read(buffer)) > 0) {
			zos.write(buffer, 0, len);
		}
		fis.close();
		zos.closeEntry();
	}
	
	private void zipDirectory(File folder, String parentPath, ZipOutputStream zos) throws Exception {
		for(File file : folder.listFiles()) {
			String ZipEntryName = parentPath + "/" + file.getName();
			if(file.isDirectory()) {
				zipDirectory(file, ZipEntryName, zos);
			}
			else {
				zip(file, parentPath, zos);
			}
		}
	}
}
