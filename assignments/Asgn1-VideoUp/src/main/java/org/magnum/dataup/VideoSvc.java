/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoSvc {

	private List<Video> mVideoCollection = new ArrayList<Video>();

	private static final AtomicLong currentId = new AtomicLong(0L);

	private Map<Long, Video> videos = new HashMap<Long, Video>();

	private static VideoFileManager videoDataMgr;

	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody List<Video> getVideoList() {
		return mVideoCollection;
	}

	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video mVideo) {

		// save video object to take note of generated IDs
		save(mVideo);

		// generate the data url for the video
		String mVideoDataURL = getDataUrl(mVideo.getId());

		// take note of video data url
		mVideo.setDataUrl(mVideoDataURL);

		// add video to videos collection
		mVideoCollection.add(mVideo);

		// return the modified by server video object
		return mVideo;
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable("id") long mVideoId,
			@RequestParam("data") MultipartFile mVideoData) {

		try {
			videoDataMgr = VideoFileManager.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Video mVideo = videos.get(mVideoId);

		try {
			saveSomeVideo(mVideo, mVideoData);

			// return video status
			return new VideoStatus(VideoState.READY);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			// return an HTTP error status
			throw new VideoNotFoundException();
		} catch (NullPointerException e) {
			// return an HTTP error status
			throw new VideoNotFoundException();
		}

	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public VideoFileManager getData(HttpServletResponse response,
			@PathVariable("id") long mVideoId) {

		// get selected video object
		Video mVideo = videos.get(mVideoId);

		try {
			serveSomeVideo(mVideo, response);

			// return the video
			return videoDataMgr.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new VideoNotFoundException();
		} catch (NullPointerException e) {
			// return an HTTP error status
			throw new VideoNotFoundException();
		}

	}

	// Used to create an id and save the Video object
	public Video save(Video entity) {
		checkAndSetId(entity);
		videos.put(entity.getId(), entity);
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if (entity.getId() == 0) {
			entity.setId(currentId.incrementAndGet());
		}
	}

	private String getDataUrl(long videoId) {
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}

	// You would need some Controller method to call this...
	public void saveSomeVideo(Video v, MultipartFile videoData)
			throws IOException {
		videoDataMgr.saveVideoData(v, videoData.getInputStream());
	}

	public void serveSomeVideo(Video v, HttpServletResponse response)
			throws IOException {
		// Of course, you would need to send some headers, etc. to the
		// client too!
		// ...
		videoDataMgr.copyVideoData(v, response.getOutputStream());
	}

	@ResponseStatus(value=HttpStatus.NOT_FOUND, reason="non-existant video")  // 404
    public class VideoNotFoundException extends RuntimeException {
        // ...
    }

}
