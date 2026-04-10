export function formatBytes(bytes) {
  if (!bytes && bytes !== 0) {
    return "";
  }
  if (bytes >= 1024 * 1024) {
    return `${Math.round(bytes / (1024 * 1024))} MB`;
  }
  if (bytes >= 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${bytes} bytes`;
}

export function groupItemsByCategory(items = []) {
  return items.reduce((groups, item) => {
    const category = item.category || "Uncategorized";
    if (!groups[category]) {
      groups[category] = [];
    }

    groups[category].push(item);
    return groups;
  }, {});
}

export function downloadTextFile(filename, content) {
  const blob = new Blob([content], { type: "text/plain" });
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(url);
}

export function buildPerFileDownloadName(fileName) {
  return fileName.replace(/\.txt$/i, "") + "-sorted.txt";
}

export function buildPerFileTextContent(file) {
  const groupedItems = groupItemsByCategory(file.items);
  let textContent = `${file.fileName}\n`;
  textContent += `${"=".repeat(file.fileName.length)}\n\n`;

  Object.entries(groupedItems).forEach(([category, items]) => {
    textContent += `${category}\n`;
    textContent += `${"-".repeat(category.length)}\n`;

    items.forEach((item) => {
      textContent += `- ${item.value}\n`;
    });

    textContent += `\n`;
  });

  return textContent;
}

export function buildMergedCategories(responseData) {
  if (responseData?.files?.length) {
    const grouped = new Map();

    responseData.files.forEach((file) => {
      file.items?.forEach((item) => {
        const category = item.category || "Other";

        if (!grouped.has(category)) {
          grouped.set(category, []);
        }

        grouped.get(category).push(item.value);
      });
    });

    return Array.from(grouped.entries()).map(([key, values]) => ({
      category: key,
      values,
    }));
  }

  return responseData?.combinedCategories || [];
}

export function normalizeResponseData(data) {
  if (!data?.files?.length) {
    return data;
  }

  return {
    ...data,
    files: data.files.map((file, fileIndex) => ({
      ...file,
      items: (file.items || []).map((item, itemIndex) => ({
        ...item,
        clientId:
          item.clientId ||
          `${file.fileName || "file"}-${fileIndex}-${itemIndex}-${item.value || "item"}`,
      })),
    })),
  };
}
